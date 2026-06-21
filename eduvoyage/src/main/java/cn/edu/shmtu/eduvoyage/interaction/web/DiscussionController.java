package cn.edu.shmtu.eduvoyage.interaction.web;

import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.DiscussionResponse;
import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.PostRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.ReplyRequest;
import cn.edu.shmtu.eduvoyage.interaction.service.DiscussionService;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "课程讨论", description = "课程讨论区发帖、回复、点赞")
@RestController
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @Operation(summary = "讨论帖列表")
    @PreAuthorize("hasAuthority('discussion:read')")
    @GetMapping("/api/courses/{courseId}/discussions")
    public Mono<Result<PageResult<DiscussionResponse>>> list(@PathVariable Long courseId,
                                                             @RequestParam(required = false) Long nodeId,
                                                             @RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "20") int pageSize,
                                                             @AuthenticationPrincipal AuthUser user) {
        return discussionService.list(courseId, nodeId, pageNo, pageSize, user).map(Result::success);
    }

    @Operation(summary = "发布讨论帖")
    @PreAuthorize("hasAuthority('discussion:write')")
    @PostMapping("/api/courses/{courseId}/discussions")
    public Mono<Result<DiscussionResponse>> create(@PathVariable Long courseId,
                                                   @Valid @RequestBody PostRequest req,
                                                   @AuthenticationPrincipal AuthUser user) {
        return discussionService.createPost(courseId, req, user).map(Result::success);
    }

    @Operation(summary = "回复列表")
    @PreAuthorize("hasAuthority('discussion:read')")
    @GetMapping("/api/discussions/{id}/replies")
    public Mono<Result<List<DiscussionResponse>>> replies(@PathVariable String id,
                                                          @AuthenticationPrincipal AuthUser user) {
        return discussionService.replies(id, user).map(Result::success);
    }

    @Operation(summary = "回复讨论")
    @PreAuthorize("hasAuthority('discussion:write')")
    @PostMapping("/api/discussions/{id}/replies")
    public Mono<Result<DiscussionResponse>> reply(@PathVariable String id,
                                                  @Valid @RequestBody ReplyRequest req,
                                                  @AuthenticationPrincipal AuthUser user) {
        return discussionService.reply(id, req, user).map(Result::success);
    }

    @Operation(summary = "点赞/取消点赞")
    @PreAuthorize("hasAuthority('discussion:write')")
    @PostMapping("/api/discussions/{id}/like")
    public Mono<Result<DiscussionResponse>> like(@PathVariable String id,
                                                 @AuthenticationPrincipal AuthUser user) {
        return discussionService.toggleLike(id, user).map(Result::success);
    }

    @Operation(summary = "删除讨论")
    @PreAuthorize("hasAuthority('discussion:write')")
    @DeleteMapping("/api/discussions/{id}")
    public Mono<Result<Void>> delete(@PathVariable String id,
                                     @AuthenticationPrincipal AuthUser user) {
        return discussionService.delete(id, user).thenReturn(Result.<Void>success());
    }
}
