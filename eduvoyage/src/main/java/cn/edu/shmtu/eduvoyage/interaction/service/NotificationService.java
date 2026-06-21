package cn.edu.shmtu.eduvoyage.interaction.service;

import cn.edu.shmtu.eduvoyage.course.domain.CourseEnrollment;
import cn.edu.shmtu.eduvoyage.course.repository.CourseEnrollmentRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.interaction.domain.Notification;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.AnnouncementRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.AnnouncementResult;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.DirectMessageRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.MarkReadResult;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.NotificationEvent;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.NotificationResponse;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.UnreadCountResponse;
import cn.edu.shmtu.eduvoyage.interaction.repository.NotificationQueryRepository;
import cn.edu.shmtu.eduvoyage.interaction.repository.NotificationRepository;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Notification persistence plus in-memory SSE fan-out. MongoDB remains the
 * source of truth, so missed SSE events are recovered by listing unread items.
 */
@Service
public class NotificationService {

    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String TYPE_DIRECT = "DIRECT";
    public static final String CATEGORY_COURSE = "COURSE";
    public static final String CATEGORY_SYSTEM = "SYSTEM";

    private static final String ROLE_ADMIN = "ADMIN";

    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository queryRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final Clock clock;
    private final Sinks.Many<NotificationEvent> sink = Sinks.many().multicast().directBestEffort();

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationQueryRepository queryRepository,
                               CourseEnrollmentRepository enrollmentRepository,
                               CourseService courseService,
                               Clock clock) {
        this.notificationRepository = notificationRepository;
        this.queryRepository = queryRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
        this.clock = clock;
    }

    public Mono<PageResult<NotificationResponse>> list(Boolean read, String category,
                                                       int pageNo, int pageSize, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        int safeNo = Math.max(1, pageNo);
        int safeSize = Math.min(Math.max(1, pageSize), 100);
        int offset = (safeNo - 1) * safeSize;
        Mono<List<NotificationResponse>> rows = queryRepository.page(user.id(), read, category, offset, safeSize)
                .map(NotificationResponse::from)
                .collectList();
        Mono<Long> total = queryRepository.count(user.id(), read, category);
        return Mono.zip(rows, total)
                .map(t -> PageResult.of(t.getT1(), t.getT2(), safeNo, safeSize));
    }

    public Mono<UnreadCountResponse> unreadCount(AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        return unreadCount(user.id()).map(UnreadCountResponse::new);
    }

    public Mono<NotificationResponse> markRead(String id, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        return notificationRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.NOTIFICATION_NOT_FOUND)))
                .flatMap(notification -> {
                    if (!notification.getToUserId().equals(user.id())) {
                        return Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权操作该通知"));
                    }
                    notification.setRead(true);
                    return notificationRepository.save(notification);
                })
                .flatMap(saved -> emitUnread(saved.getToUserId()).thenReturn(saved))
                .map(NotificationResponse::from);
    }

    public Mono<MarkReadResult> markAllRead(String category, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        return queryRepository.markAllRead(user.id(), category)
                .flatMap(modified -> emitUnread(user.id()).thenReturn(new MarkReadResult(modified)));
    }

    public Mono<AnnouncementResult> announce(Long courseId, AnnouncementRequest req, AuthUser sender) {
        if (sender == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        BizException titleError = InteractionRules.validateTitle(req.title());
        if (titleError != null) {
            return Mono.error(titleError);
        }
        BizException bodyError = InteractionRules.validateContent(req.body(), 4000);
        if (bodyError != null) {
            return Mono.error(bodyError);
        }
        return courseService.requireCourseEditable(courseId, sender)
                .thenMany(enrollmentRepository.findActiveByCourse(courseId)
                        .map(CourseEnrollment::getStudentId)
                        .distinct()
                        .concatMap(studentId -> create(studentId, TYPE_ANNOUNCEMENT,
                                InteractionRules.normalize(req.title()),
                                InteractionRules.normalize(req.body()),
                                String.valueOf(courseId),
                                CATEGORY_COURSE)))
                .count()
                .map(AnnouncementResult::new);
    }

    public Mono<NotificationResponse> direct(DirectMessageRequest req, AuthUser sender) {
        if (sender == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        if (!sender.hasRole(ROLE_ADMIN)) {
            return Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "仅管理员可发送站内信"));
        }
        BizException titleError = InteractionRules.validateTitle(req.title());
        if (titleError != null) {
            return Mono.error(titleError);
        }
        BizException bodyError = InteractionRules.validateContent(req.body(), 4000);
        if (bodyError != null) {
            return Mono.error(bodyError);
        }
        return create(req.toUserId(), TYPE_DIRECT, InteractionRules.normalize(req.title()),
                InteractionRules.normalize(req.body()), req.refId(),
                req.category() == null || req.category().isBlank() ? CATEGORY_SYSTEM : req.category().trim())
                .map(NotificationResponse::from);
    }

    public Mono<Notification> create(Long toUserId, String type, String title,
                                     String body, String refId, String category) {
        if (toUserId == null) {
            return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "接收人不能为空"));
        }
        Notification notification = Notification.builder()
                .toUserId(toUserId)
                .type(type)
                .title(title)
                .body(body)
                .refId(refId)
                .category(category)
                .read(false)
                .ts(now())
                .deleted(false)
                .build();
        return notificationRepository.save(notification)
                .flatMap(saved -> unreadCount(saved.getToUserId())
                        .doOnNext(count -> emit(NotificationEvent.notification(saved, count, now())))
                        .thenReturn(saved));
    }

    public Flux<ServerSentEvent<NotificationEvent>> stream(AuthUser user) {
        if (user == null) {
            return Flux.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        Flux<ServerSentEvent<NotificationEvent>> initial = unreadCount(user.id())
                .map(count -> toSse(NotificationEvent.unread(user.id(), count, now())))
                .flux();
        Flux<ServerSentEvent<NotificationEvent>> live = sink.asFlux()
                .filter(event -> event.toUserId() != null && event.toUserId().equals(user.id()))
                .map(this::toSse);
        Flux<ServerSentEvent<NotificationEvent>> heartbeat = Flux.interval(Duration.ofSeconds(25))
                .map(i -> ServerSentEvent.<NotificationEvent>builder().comment("heartbeat").build());
        return Flux.concat(initial, Flux.merge(live, heartbeat));
    }

    private Mono<Long> unreadCount(Long userId) {
        return notificationRepository.countByToUserIdAndReadFalseAndDeletedFalse(userId);
    }

    private Mono<Void> emitUnread(Long userId) {
        return unreadCount(userId)
                .doOnNext(count -> emit(NotificationEvent.unread(userId, count, now())))
                .then();
    }

    private void emit(NotificationEvent event) {
        sink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    private ServerSentEvent<NotificationEvent> toSse(NotificationEvent event) {
        return ServerSentEvent.<NotificationEvent>builder(event)
                .event(event.event())
                .id(event.event() + ":" + event.ts().toEpochMilli())
                .build();
    }

    private Instant now() {
        return clock.instant();
    }
}
