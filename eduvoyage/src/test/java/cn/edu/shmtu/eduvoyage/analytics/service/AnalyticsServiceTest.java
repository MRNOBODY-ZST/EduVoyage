package cn.edu.shmtu.eduvoyage.analytics.service;

import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.LearningLogRequest;
import cn.edu.shmtu.eduvoyage.analytics.repository.AnalyticsQueryRepository;
import cn.edu.shmtu.eduvoyage.analytics.repository.LearningLogQueryRepository;
import cn.edu.shmtu.eduvoyage.analytics.repository.LearningLogRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseAccessService;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock LearningLogRepository learningLogRepository;
    @Mock LearningLogQueryRepository logQueryRepository;
    @Mock AnalyticsQueryRepository analyticsQueryRepository;
    @Mock CourseAccessService courseAccessService;
    @Mock CourseService courseService;

    private AnalyticsService service;

    private static final AuthUser STUDENT = new AuthUser(3L, "student", Set.of("STUDENT"), Set.of("analytics:view"));

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(learningLogRepository, logQueryRepository, analyticsQueryRepository,
                courseAccessService, courseService,
                Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    void recordLogRejectsInvalidDurationBeforeCourseLookup() {
        LearningLogRequest req = new LearningLogRequest(1L, null, "STUDY", 86_401);

        StepVerifier.create(service.recordLog(req, STUDENT))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.LEARNING_LOG_INVALID))
                .verify();
    }

    @Test
    void adminDashboardRequiresAdminRole() {
        StepVerifier.create(service.adminDashboard(STUDENT))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ANALYTICS_SCOPE_INVALID))
                .verify();
    }
}
