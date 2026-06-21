package cn.edu.shmtu.eduvoyage.interaction.service;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.domain.CourseEnrollment;
import cn.edu.shmtu.eduvoyage.course.repository.CourseEnrollmentRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.interaction.domain.Notification;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.AnnouncementRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.DirectMessageRequest;
import cn.edu.shmtu.eduvoyage.interaction.repository.NotificationQueryRepository;
import cn.edu.shmtu.eduvoyage.interaction.repository.NotificationRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationQueryRepository queryRepository;
    @Mock CourseEnrollmentRepository enrollmentRepository;
    @Mock CourseService courseService;

    private NotificationService service;

    private static final AuthUser ADMIN = new AuthUser(1L, "admin", Set.of("ADMIN"), Set.of("notification:write"));
    private static final AuthUser TEACHER = new AuthUser(2L, "teacher", Set.of("TEACHER"), Set.of("notification:write"));

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, queryRepository, enrollmentRepository, courseService,
                Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    void directMessageRequiresAdmin() {
        DirectMessageRequest req = new DirectMessageRequest(3L, "title", "body", null, null);

        StepVerifier.create(service.direct(req, TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ACCESS_DENIED))
                .verify();
    }

    @Test
    void announcementCreatesOneNotificationPerEnrolledStudent() {
        when(courseService.requireCourseEditable(10L, TEACHER))
                .thenReturn(Mono.just(Course.builder().id(10L).teacherId(TEACHER.id()).build()));
        when(enrollmentRepository.findActiveByCourse(10L)).thenReturn(Flux.just(
                CourseEnrollment.builder().studentId(3L).status(CourseEnrollment.STATUS_ENROLLED).build(),
                CourseEnrollment.builder().studentId(4L).status(CourseEnrollment.STATUS_ENROLLED).build()));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId("n-" + n.getToUserId());
            return Mono.just(n);
        });
        when(notificationRepository.countByToUserIdAndReadFalseAndDeletedFalse(any())).thenReturn(Mono.just(1L));

        StepVerifier.create(service.announce(10L, new AnnouncementRequest("公告", "内容"), TEACHER))
                .assertNext(result -> assertThat(result.recipientCount()).isEqualTo(2))
                .verifyComplete();
    }

    @Test
    void adminCanSendDirectMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId("n1");
            return Mono.just(n);
        });
        when(notificationRepository.countByToUserIdAndReadFalseAndDeletedFalse(3L)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.direct(new DirectMessageRequest(3L, "系统", "消息", null, null), ADMIN))
                .assertNext(resp -> {
                    assertThat(resp.toUserId()).isEqualTo(3L);
                    assertThat(resp.category()).isEqualTo(NotificationService.CATEGORY_SYSTEM);
                })
                .verifyComplete();
    }
}
