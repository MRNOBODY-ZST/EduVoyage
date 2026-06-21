package cn.edu.shmtu.eduvoyage.course.web;

import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseResponse;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Course catalog and lifecycle. Reads are open to any authenticated user;
 * authoring requires {@code course:create}/{@code course:update}/{@code
 * course:delete}, plus a per-course ownership check enforced in the service.
 */
@Tag(name = "课程管理", description = "课程的检索、详情、增删改与发布")
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "课程列表（分页，可按关键字/教师/状态/可见性过滤）")
    @GetMapping
    public Mono<Result<PageResult<CourseResponse>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer visibility,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return courseService.page(keyword, teacherId, status, visibility, pageNo, pageSize)
                .map(Result::success);
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public Mono<Result<CourseResponse>> get(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthUser user) {
        return courseService.get(id, user).map(Result::success);
    }

    @Operation(summary = "创建课程")
    @PreAuthorize("hasAuthority('course:create')")
    @PostMapping
    public Mono<Result<CourseResponse>> create(@Valid @RequestBody CourseRequest req,
                                               @AuthenticationPrincipal AuthUser user) {
        return courseService.create(req, user.id()).map(Result::success);
    }

    @Operation(summary = "更新课程")
    @PreAuthorize("hasAuthority('course:update')")
    @PutMapping("/{id}")
    public Mono<Result<CourseResponse>> update(@PathVariable Long id,
                                               @Valid @RequestBody CourseRequest req,
                                               @AuthenticationPrincipal AuthUser user) {
        return courseService.update(id, req, user).map(Result::success);
    }

    @Operation(summary = "发布课程")
    @PreAuthorize("hasAuthority('course:update')")
    @PostMapping("/{id}/publish")
    public Mono<Result<CourseResponse>> publish(@PathVariable Long id,
                                                @AuthenticationPrincipal AuthUser user) {
        return courseService.publish(id, user).map(Result::success);
    }

    @Operation(summary = "归档课程")
    @PreAuthorize("hasAuthority('course:update')")
    @PostMapping("/{id}/archive")
    public Mono<Result<CourseResponse>> archive(@PathVariable Long id,
                                                @AuthenticationPrincipal AuthUser user) {
        return courseService.archive(id, user).map(Result::success);
    }

    @Operation(summary = "删除课程")
    @PreAuthorize("hasAuthority('course:delete')")
    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser user) {
        return courseService.delete(id, user).thenReturn(Result.<Void>success());
    }
}
