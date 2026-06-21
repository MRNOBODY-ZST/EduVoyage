package cn.edu.shmtu.eduvoyage.course.web;

import cn.edu.shmtu.eduvoyage.course.dto.EnrollmentDtos.EnrollmentResponse;
import cn.edu.shmtu.eduvoyage.course.dto.EnrollmentDtos.ProgressRequest;
import cn.edu.shmtu.eduvoyage.course.service.EnrollmentService;
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

/**
 * Student-facing enrollment, progress and favorites. All endpoints require the
 * {@code course:enroll} permission (granted to STUDENT) and act on the
 * authenticated principal — a student can only manage their own records.
 */
@Tag(name = "选课与学习", description = "学生选课、退课、学习进度与收藏")
@RestController
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "选课")
    @PreAuthorize("hasAuthority('course:enroll')")
    @PostMapping("/api/courses/{courseId}/enroll")
    public Mono<Result<EnrollmentResponse>> enroll(@PathVariable Long courseId,
                                                   @AuthenticationPrincipal AuthUser user) {
        return enrollmentService.enroll(courseId, user.id()).map(Result::success);
    }

    @Operation(summary = "退课")
    @PreAuthorize("hasAuthority('course:enroll')")
    @DeleteMapping("/api/courses/{courseId}/enroll")
    public Mono<Result<Void>> drop(@PathVariable Long courseId,
                                   @AuthenticationPrincipal AuthUser user) {
        return enrollmentService.drop(courseId, user.id()).thenReturn(Result.<Void>success());
    }

    @Operation(summary = "我的选课记录")
    @PreAuthorize("hasAuthority('course:enroll')")
    @GetMapping("/api/courses/{courseId}/enroll/me")
    public Mono<Result<EnrollmentResponse>> myEnrollment(@PathVariable Long courseId,
                                                         @AuthenticationPrincipal AuthUser user) {
        return enrollmentService.myEnrollment(courseId, user.id()).map(Result::success);
    }

    @Operation(summary = "更新学习进度")
    @PreAuthorize("hasAuthority('course:enroll')")
    @PutMapping("/api/courses/{courseId}/enroll/progress")
    public Mono<Result<EnrollmentResponse>> updateProgress(@PathVariable Long courseId,
                                                           @Valid @RequestBody ProgressRequest req,
                                                           @AuthenticationPrincipal AuthUser user) {
        return enrollmentService.updateProgress(courseId, user.id(), req.progress()).map(Result::success);
    }

    @Operation(summary = "收藏课程")
    @PreAuthorize("hasAuthority('course:enroll')")
    @PostMapping("/api/courses/{courseId}/favorite")
    public Mono<Result<Void>> favorite(@PathVariable Long courseId,
                                       @AuthenticationPrincipal AuthUser user) {
        return enrollmentService.favorite(courseId, user.id()).thenReturn(Result.<Void>success());
    }

    @Operation(summary = "取消收藏")
    @PreAuthorize("hasAuthority('course:enroll')")
    @DeleteMapping("/api/courses/{courseId}/favorite")
    public Mono<Result<Void>> unfavorite(@PathVariable Long courseId,
                                         @AuthenticationPrincipal AuthUser user) {
        return enrollmentService.unfavorite(courseId, user.id()).thenReturn(Result.<Void>success());
    }
}
