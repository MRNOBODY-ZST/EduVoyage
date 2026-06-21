package cn.edu.shmtu.eduvoyage.analytics.service;

import cn.edu.shmtu.eduvoyage.analytics.domain.LearningLog;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.AdminDashboardResponse;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.CourseAnalyticsResponse;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.GradeTrendPoint;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.HomeworkStat;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.LearningLogRequest;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.LearningLogResponse;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.NodeMastery;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.StudentDashboardResponse;
import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.StudentRanking;
import cn.edu.shmtu.eduvoyage.analytics.repository.AnalyticsQueryRepository;
import cn.edu.shmtu.eduvoyage.analytics.repository.LearningLogQueryRepository;
import cn.edu.shmtu.eduvoyage.analytics.repository.LearningLogRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseAccessService;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AnalyticsService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final int RECENT_LOG_LIMIT = 10;
    private static final int TREND_LIMIT = 12;
    private static final int RANKING_LIMIT = 20;
    private static final int WEAK_NODE_LIMIT = 5;

    private final LearningLogRepository learningLogRepository;
    private final LearningLogQueryRepository logQueryRepository;
    private final AnalyticsQueryRepository analyticsQueryRepository;
    private final CourseAccessService courseAccessService;
    private final CourseService courseService;
    private final Clock clock;

    public AnalyticsService(LearningLogRepository learningLogRepository,
                            LearningLogQueryRepository logQueryRepository,
                            AnalyticsQueryRepository analyticsQueryRepository,
                            CourseAccessService courseAccessService,
                            CourseService courseService,
                            Clock clock) {
        this.learningLogRepository = learningLogRepository;
        this.logQueryRepository = logQueryRepository;
        this.analyticsQueryRepository = analyticsQueryRepository;
        this.courseAccessService = courseAccessService;
        this.courseService = courseService;
        this.clock = clock;
    }

    public Mono<LearningLogResponse> recordLog(LearningLogRequest req, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        BizException error = AnalyticsRules.validateLog(req.action(), req.durationSec());
        if (error != null) {
            return Mono.error(error);
        }
        return courseAccessService.requireParticipant(req.courseId(), user)
                .then(Mono.defer(() -> {
                    LearningLog log = LearningLog.builder()
                            .userId(user.id())
                            .courseId(req.courseId())
                            .nodeId(req.nodeId())
                            .action(AnalyticsRules.normalizeAction(req.action()))
                            .durationSec(req.durationSec() == null ? 0 : req.durationSec())
                            .ts(now())
                            .build();
                    return learningLogRepository.save(log);
                }))
                .map(LearningLogResponse::from);
    }

    public Mono<StudentDashboardResponse> myDashboard(AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        Long studentId = user.id();
        Instant since = now().minusSeconds(30L * 24 * 60 * 60);
        Mono<Long> totalDuration = logQueryRepository.sumDuration(studentId, null, since);
        Mono<Long> activeDays = logQueryRepository.recentByUserSince(studentId, since)
                .map(log -> LocalDate.ofInstant(log.getTs(), clock.getZone()))
                .distinct()
                .count();
        Mono<Long> enrolled = analyticsQueryRepository.countEnrolledCourses(studentId);
        Mono<Long> todo = analyticsQueryRepository.countTodoHomeworks(studentId);
        Mono<BigDecimal> average = analyticsQueryRepository.averageStudentScore(studentId);
        Mono<BigDecimal> mastery = analyticsQueryRepository.masteryPercent(studentId);
        Mono<List<GradeTrendPoint>> trend = analyticsQueryRepository.gradeTrend(studentId, TREND_LIMIT)
                .map(GradeTrendPoint::from)
                .collectList();
        Mono<List<LearningLogResponse>> recent = logQueryRepository.recentByUser(studentId, RECENT_LOG_LIMIT)
                .map(LearningLogResponse::from)
                .collectList();

        return Mono.zip(totalDuration, activeDays, enrolled, todo, average, mastery, trend, recent)
                .map(t -> new StudentDashboardResponse(studentId, t.getT1(), t.getT2(), t.getT3(),
                        t.getT4(), scale(t.getT5()), scale(t.getT6()), t.getT7(), t.getT8()));
    }

    public Mono<CourseAnalyticsResponse> courseDashboard(Long courseId, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        Instant since = now().minusSeconds(30L * 24 * 60 * 60);
        return courseService.requireCourseEditable(courseId, user)
                .then(Mono.defer(() -> {
                    Mono<Long> enrolled = analyticsQueryRepository.countActiveEnrollments(courseId);
                    Mono<Long> active = logQueryRepository.activeUsers(courseId, since);
                    Mono<Long> duration = logQueryRepository.sumCourseDuration(courseId, since);
                    Mono<List<HomeworkStat>> homeworkStats = enrolled.flatMap(total ->
                            analyticsQueryRepository.homeworkStats(courseId)
                                    .map(row -> HomeworkStat.from(row, total))
                                    .collectList());
                    Mono<List<StudentRanking>> rankings = analyticsQueryRepository.studentRanking(courseId, RANKING_LIMIT)
                            .map(StudentRanking::from)
                            .collectList();
                    Mono<List<NodeMastery>> heatmap = analyticsQueryRepository.nodeMastery(courseId)
                            .map(NodeMastery::from)
                            .collectList();

                    return Mono.zip(enrolled, active, duration, homeworkStats, rankings, heatmap)
                            .map(t -> {
                                List<HomeworkStat> stats = t.getT4();
                                BigDecimal submissionRate = averageRate(stats.stream()
                                        .map(HomeworkStat::submissionRate)
                                        .toList());
                                BigDecimal averageScore = averageRate(stats.stream()
                                        .map(HomeworkStat::averageScore)
                                        .filter(score -> score.compareTo(BigDecimal.ZERO) > 0)
                                        .toList());
                                List<NodeMastery> weakNodes = t.getT6().stream()
                                        .sorted(Comparator.comparing(NodeMastery::averageProgress))
                                        .limit(WEAK_NODE_LIMIT)
                                        .toList();
                                return new CourseAnalyticsResponse(courseId, t.getT1(), t.getT2(), t.getT3(),
                                        submissionRate, averageScore, stats, t.getT5(), t.getT6(), weakNodes);
                            });
                }));
    }

    public Mono<AdminDashboardResponse> adminDashboard(AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        if (!user.hasRole(ROLE_ADMIN)) {
            return Mono.error(new BizException(BizErrorCode.ANALYTICS_SCOPE_INVALID));
        }
        Instant since = now().minusSeconds(30L * 24 * 60 * 60);
        LocalDateTime sinceLocal = LocalDateTime.ofInstant(since, clock.getZone());
        return Mono.zip(
                        analyticsQueryRepository.totalUsers(),
                        logQueryRepository.activeUsers(null, since),
                        analyticsQueryRepository.newUsersSince(sinceLocal),
                        analyticsQueryRepository.totalCourses(),
                        analyticsQueryRepository.totalHomeworks(),
                        analyticsQueryRepository.totalSubmissions(),
                        analyticsQueryRepository.storageUsedBytes())
                .map(t -> new AdminDashboardResponse(t.getT1(), t.getT2(), t.getT3(),
                        t.getT4(), t.getT5(), t.getT6(), t.getT7()));
    }

    private Instant now() {
        return clock.instant();
    }

    private static BigDecimal averageRate(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
