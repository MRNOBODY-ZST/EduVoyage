package cn.edu.shmtu.eduvoyage.course.web;

import cn.edu.shmtu.eduvoyage.course.dto.CoursewareDtos.CoursewareRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CoursewareDtos.CoursewareResponse;
import cn.edu.shmtu.eduvoyage.course.service.CoursewareService;
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
 * Courseware attached to a knowledge node. Reads are open to authenticated users
 * (students view materials); authoring requires {@code course:update} plus the
 * service-level course ownership check.
 */
@Tag(name = "课件管理", description = "知识点下的课件资源")
@RestController
public class CoursewareController {

    private final CoursewareService coursewareService;

    public CoursewareController(CoursewareService coursewareService) {
        this.coursewareService = coursewareService;
    }

    @Operation(summary = "知识点课件列表")
    @GetMapping("/api/nodes/{nodeId}/coursewares")
    public Mono<Result<List<CoursewareResponse>>> listByNode(@PathVariable Long nodeId) {
        return coursewareService.listByNode(nodeId).map(Result::success);
    }

    @Operation(summary = "新增课件")
    @PreAuthorize("hasAuthority('course:update')")
    @PostMapping("/api/nodes/{nodeId}/coursewares")
    public Mono<Result<CoursewareResponse>> create(@PathVariable Long nodeId,
                                                   @Valid @RequestBody CoursewareRequest req,
                                                   @AuthenticationPrincipal AuthUser user) {
        return coursewareService.create(nodeId, req, user).map(Result::success);
    }

    @Operation(summary = "更新课件")
    @PreAuthorize("hasAuthority('course:update')")
    @PutMapping("/api/coursewares/{id}")
    public Mono<Result<CoursewareResponse>> update(@PathVariable Long id,
                                                   @Valid @RequestBody CoursewareRequest req,
                                                   @AuthenticationPrincipal AuthUser user) {
        return coursewareService.update(id, req, user).map(Result::success);
    }

    @Operation(summary = "删除课件")
    @PreAuthorize("hasAuthority('course:update')")
    @DeleteMapping("/api/coursewares/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser user) {
        return coursewareService.delete(id, user).thenReturn(Result.<Void>success());
    }
}
