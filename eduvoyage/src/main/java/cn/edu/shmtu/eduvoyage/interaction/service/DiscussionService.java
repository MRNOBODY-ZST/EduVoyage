package cn.edu.shmtu.eduvoyage.interaction.service;

import cn.edu.shmtu.eduvoyage.course.service.CourseAccessService;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.interaction.domain.Discussion;
import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.DiscussionResponse;
import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.PostRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.ReplyRequest;
import cn.edu.shmtu.eduvoyage.interaction.repository.DiscussionQueryRepository;
import cn.edu.shmtu.eduvoyage.interaction.repository.DiscussionRepository;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/**
 * Course discussion posts, replies and likes stored in MongoDB.
 */
@Service
public class DiscussionService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final DiscussionRepository discussionRepository;
    private final DiscussionQueryRepository queryRepository;
    private final CourseAccessService courseAccessService;
    private final CourseService courseService;
    private final Clock clock;

    public DiscussionService(DiscussionRepository discussionRepository,
                             DiscussionQueryRepository queryRepository,
                             CourseAccessService courseAccessService,
                             CourseService courseService,
                             Clock clock) {
        this.discussionRepository = discussionRepository;
        this.queryRepository = queryRepository;
        this.courseAccessService = courseAccessService;
        this.courseService = courseService;
        this.clock = clock;
    }

    public Mono<PageResult<DiscussionResponse>> list(Long courseId, Long nodeId,
                                                     int pageNo, int pageSize, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        int safeNo = Math.max(1, pageNo);
        int safeSize = Math.min(Math.max(1, pageSize), 100);
        int offset = (safeNo - 1) * safeSize;
        Mono<List<DiscussionResponse>> rows = courseAccessService.requireParticipant(courseId, user)
                .thenMany(queryRepository.pageRootPosts(courseId, nodeId, offset, safeSize))
                .map(d -> DiscussionResponse.from(d, user.id()))
                .collectList();
        Mono<Long> total = courseAccessService.requireParticipant(courseId, user)
                .then(queryRepository.countRootPosts(courseId, nodeId));
        return Mono.zip(rows, total)
                .map(t -> PageResult.of(t.getT1(), t.getT2(), safeNo, safeSize));
    }

    public Mono<DiscussionResponse> createPost(Long courseId, PostRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        BizException titleError = InteractionRules.validateTitle(req.title());
        if (titleError != null) {
            return Mono.error(titleError);
        }
        BizException contentError = InteractionRules.validateContent(req.content(), 8000);
        if (contentError != null) {
            return Mono.error(contentError);
        }
        return courseAccessService.requireParticipant(courseId, user)
                .then(Mono.defer(() -> {
                    Discussion discussion = Discussion.builder()
                            .courseId(courseId)
                            .nodeId(req.nodeId())
                            .authorId(user.id())
                            .title(InteractionRules.normalize(req.title()))
                            .content(InteractionRules.normalize(req.content()))
                            .likes(new HashSet<>())
                            .replyCount(0)
                            .ts(now())
                            .deleted(false)
                            .build();
                    return discussionRepository.save(discussion);
                }))
                .map(d -> DiscussionResponse.from(d, user.id()));
    }

    public Mono<List<DiscussionResponse>> replies(String parentId, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        return requireDiscussion(parentId)
                .flatMap(parent -> courseAccessService.requireParticipant(parent.getCourseId(), user).thenReturn(parent))
                .thenMany(discussionRepository.findReplies(parentId))
                .map(d -> DiscussionResponse.from(d, user.id()))
                .collectList();
    }

    public Mono<DiscussionResponse> reply(String parentId, ReplyRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        BizException contentError = InteractionRules.validateContent(req.content(), 8000);
        if (contentError != null) {
            return Mono.error(contentError);
        }
        return requireDiscussion(parentId)
                .flatMap(parent -> courseAccessService.requireParticipant(parent.getCourseId(), user).thenReturn(parent))
                .flatMap(parent -> {
                    Discussion reply = Discussion.builder()
                            .courseId(parent.getCourseId())
                            .nodeId(parent.getNodeId())
                            .authorId(user.id())
                            .content(InteractionRules.normalize(req.content()))
                            .parentId(parent.getId())
                            .likes(new HashSet<>())
                            .replyCount(0)
                            .ts(now())
                            .deleted(false)
                            .build();
                    parent.setReplyCount(parent.getReplyCount() + 1);
                    return discussionRepository.save(reply)
                            .flatMap(saved -> discussionRepository.save(parent).thenReturn(saved));
                })
                .map(d -> DiscussionResponse.from(d, user.id()));
    }

    public Mono<DiscussionResponse> toggleLike(String id, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        return requireDiscussion(id)
                .flatMap(discussion -> courseAccessService.requireParticipant(discussion.getCourseId(), user)
                        .thenReturn(discussion))
                .flatMap(discussion -> {
                    if (discussion.getLikes() == null) {
                        discussion.setLikes(new HashSet<>());
                    }
                    if (!discussion.getLikes().add(user.id())) {
                        discussion.getLikes().remove(user.id());
                    }
                    return discussionRepository.save(discussion);
                })
                .map(d -> DiscussionResponse.from(d, user.id()));
    }

    public Mono<Void> delete(String id, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        return requireDiscussion(id)
                .flatMap(discussion -> canDelete(discussion, user)
                        .flatMap(ok -> ok ? Mono.just(discussion)
                                : Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权删除该讨论"))))
                .flatMap(discussion -> {
                    discussion.setDeleted(true);
                    return discussionRepository.save(discussion);
                })
                .then();
    }

    private Mono<Boolean> canDelete(Discussion discussion, AuthUser user) {
        if (user.hasRole(ROLE_ADMIN) || discussion.getAuthorId().equals(user.id())) {
            return Mono.just(true);
        }
        return courseService.requireCourseEditable(discussion.getCourseId(), user)
                .thenReturn(true)
                .onErrorResume(BizException.class, e -> e.getErrorCode() == BizErrorCode.ACCESS_DENIED
                        ? Mono.just(false)
                        : Mono.error(e));
    }

    private Mono<Discussion> requireDiscussion(String id) {
        return discussionRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.DISCUSSION_NOT_FOUND)));
    }

    private Instant now() {
        return clock.instant();
    }
}
