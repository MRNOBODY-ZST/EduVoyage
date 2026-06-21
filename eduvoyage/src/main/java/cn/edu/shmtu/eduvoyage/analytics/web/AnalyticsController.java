package cn.edu.shmtu.eduvoyage.analytics.web;

import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.AdminDashboardResponse;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.CourseAnalyticsResponse;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.LearningLogRequest;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.LearningLogResponse;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.StudentDashboardResponse;
import cn.edu.shmtu.eduvoyage.analytics.service.AnalyticsService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "学习分析", description = "学习行为采集与学情/教学/运营分析")
@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "上报学习行为")
    @PreAuthorize("hasAuthority('analytics:view')")
    @PostMapping("/api/analytics/logs")
    public Mono<Result<LearningLogResponse>> record(@Valid @RequestBody LearningLogRequest req,
                                                    @AuthenticationPrincipal AuthUser user) {
        return analyticsService.recordLog(req, user).map(Result::success);
    }

    @Operation(summary = "我的学情")
    @PreAuthorize("hasAuthority('analytics:view')")
    @GetMapping("/api/analytics/me")
    public Mono<Result<StudentDashboardResponse>> me(@AuthenticationPrincipal AuthUser user) {
        return analyticsService.myDashboard(user).map(Result::success);
    }

    @Operation(summary = "课程教学分析")
    @PreAuthorize("hasAuthority('analytics:view')")
    @GetMapping("/api/analytics/courses/{courseId}")
    public Mono<Result<CourseAnalyticsResponse>> course(@PathVariable Long courseId,
                                                        @AuthenticationPrincipal AuthUser user) {
        return analyticsService.courseDashboard(courseId, user).map(Result::success);
    }

    @Operation(summary = "平台运营大盘")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('analytics:view')")
    @GetMapping("/api/analytics/admin")
    public Mono<Result<AdminDashboardResponse>> admin(@AuthenticationPrincipal AuthUser user) {
        return analyticsService.adminDashboard(user).map(Result::success);
    }
}
