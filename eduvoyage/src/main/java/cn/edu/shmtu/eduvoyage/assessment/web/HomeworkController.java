package cn.edu.shmtu.eduvoyage.assessment.web;

import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.HomeworkRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.HomeworkResponse;
import cn.edu.shmtu.eduvoyage.assessment.service.HomeworkService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Homework (paper) management. Listing/detail requires {@code homework:read};
 * authoring, publish/close and delete require {@code homework:create} plus the
 * service-level course ownership check.
 */
@Tag(name = "作业管理", description = "作业/试卷的组卷与发布")
@RestController
public class HomeworkController {

    private final HomeworkService homeworkService;

    public HomeworkController(HomeworkService homeworkService) {
        this.homeworkService = homeworkService;
    }

    @Operation(summary = "课程作业列表")
    @PreAuthorize("hasAuthority('homework:read')")
    @GetMapping("/api/courses/{courseId}/homeworks")
    public Mono<Result<List<HomeworkResponse>>> listByCourse(@PathVariable Long courseId) {
        return homeworkService.listByCourse(courseId).map(Result::success);
    }

    @Operation(summary = "作业详情")
    @PreAuthorize("hasAuthority('homework:read')")
    @GetMapping("/api/homeworks/{id}")
    public Mono<Result<HomeworkResponse>> get(@PathVariable Long id) {
        return homeworkService.get(id).map(Result::success);
    }

    @Operation(summary = "创建作业/试卷")
    @PreAuthorize("hasAuthority('homework:create')")
    @PostMapping("/api/courses/{courseId}/homeworks")
    public Mono<Result<HomeworkResponse>> create(@PathVariable Long courseId,
                                                 @Valid @RequestBody HomeworkRequest req,
                                                 @AuthenticationPrincipal AuthUser user) {
        return homeworkService.create(courseId, req, user).map(Result::success);
    }

    @Operation(summary = "更新作业/试卷")
    @PreAuthorize("hasAuthority('homework:create')")
    @PutMapping("/api/homeworks/{id}")
    public Mono<Result<HomeworkResponse>> update(@PathVariable Long id,
                                                 @Valid @RequestBody HomeworkRequest req,
                                                 @AuthenticationPrincipal AuthUser user) {
        return homeworkService.update(id, req, user).map(Result::success);
    }

    @Operation(summary = "发布作业")
    @PreAuthorize("hasAuthority('homework:create')")
    @PostMapping("/api/homeworks/{id}/publish")
    public Mono<Result<HomeworkResponse>> publish(@PathVariable Long id,
                                                  @AuthenticationPrincipal AuthUser user) {
        return homeworkService.publish(id, user).map(Result::success);
    }

    @Operation(summary = "关闭作业")
    @PreAuthorize("hasAuthority('homework:create')")
    @PostMapping("/api/homeworks/{id}/close")
    public Mono<Result<HomeworkResponse>> close(@PathVariable Long id,
                                                @AuthenticationPrincipal AuthUser user) {
        return homeworkService.close(id, user).map(Result::success);
    }

    @Operation(summary = "删除作业")
    @PreAuthorize("hasAuthority('homework:create')")
    @DeleteMapping("/api/homeworks/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser user) {
        return homeworkService.delete(id, user).thenReturn(Result.<Void>success());
    }
}
