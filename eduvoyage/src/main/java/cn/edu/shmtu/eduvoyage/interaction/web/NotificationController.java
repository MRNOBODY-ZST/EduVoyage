package cn.edu.shmtu.eduvoyage.interaction.web;

import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.AnnouncementRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.AnnouncementResult;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.DirectMessageRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.MarkReadResult;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.NotificationEvent;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.NotificationResponse;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.UnreadCountResponse;
import cn.edu.shmtu.eduvoyage.interaction.service.NotificationService;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "通知", description = "站内通知、公告与 SSE 推送")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "我的通知")
    @PreAuthorize("hasAuthority('notification:read')")
    @GetMapping("/api/notifications")
    public Mono<Result<PageResult<NotificationResponse>>> list(@RequestParam(required = false) Boolean read,
                                                               @RequestParam(required = false) String category,
                                                               @RequestParam(defaultValue = "1") int pageNo,
                                                               @RequestParam(defaultValue = "20") int pageSize,
                                                               @AuthenticationPrincipal AuthUser user) {
        return notificationService.list(read, category, pageNo, pageSize, user).map(Result::success);
    }

    @Operation(summary = "未读数量")
    @PreAuthorize("hasAuthority('notification:read')")
    @GetMapping("/api/notifications/unread-count")
    public Mono<Result<UnreadCountResponse>> unreadCount(@AuthenticationPrincipal AuthUser user) {
        return notificationService.unreadCount(user).map(Result::success);
    }

    @Operation(summary = "标记单条已读")
    @PreAuthorize("hasAuthority('notification:read')")
    @PutMapping("/api/notifications/{id}/read")
    public Mono<Result<NotificationResponse>> markRead(@PathVariable String id,
                                                       @AuthenticationPrincipal AuthUser user) {
        return notificationService.markRead(id, user).map(Result::success);
    }

    @Operation(summary = "批量已读")
    @PreAuthorize("hasAuthority('notification:read')")
    @PutMapping("/api/notifications/read-all")
    public Mono<Result<MarkReadResult>> markAllRead(@RequestParam(required = false) String category,
                                                    @AuthenticationPrincipal AuthUser user) {
        return notificationService.markAllRead(category, user).map(Result::success);
    }

    @Operation(summary = "发布课程公告")
    @PreAuthorize("hasAuthority('notification:write')")
    @PostMapping("/api/courses/{courseId}/announcements")
    public Mono<Result<AnnouncementResult>> announce(@PathVariable Long courseId,
                                                     @Valid @RequestBody AnnouncementRequest req,
                                                     @AuthenticationPrincipal AuthUser user) {
        return notificationService.announce(courseId, req, user).map(Result::success);
    }

    @Operation(summary = "管理员发送站内信")
    @PreAuthorize("hasAuthority('notification:write')")
    @PostMapping("/api/notifications/direct")
    public Mono<Result<NotificationResponse>> direct(@Valid @RequestBody DirectMessageRequest req,
                                                     @AuthenticationPrincipal AuthUser user) {
        return notificationService.direct(req, user).map(Result::success);
    }

    @Operation(summary = "通知 SSE 流")
    @PreAuthorize("hasAuthority('notification:read')")
    @GetMapping(value = "/api/sse/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationEvent>> stream(@AuthenticationPrincipal AuthUser user) {
        return notificationService.stream(user);
    }
}
