package cn.edu.shmtu.eduvoyage.interaction;

import cn.edu.shmtu.eduvoyage.analytics.dto.AnalyticsDtos.LearningLogRequest;
import cn.edu.shmtu.eduvoyage.analytics.service.AnalyticsService;
import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseResponse;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.course.service.EnrollmentService;
import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.PostRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.DiscussionDtos.ReplyRequest;
import cn.edu.shmtu.eduvoyage.interaction.dto.NotificationDtos.AnnouncementRequest;
import cn.edu.shmtu.eduvoyage.interaction.service.DiscussionService;
import cn.edu.shmtu.eduvoyage.interaction.service.NotificationService;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class InteractionAnalyticsIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.0")
            .withDatabaseName("eduvoyage")
            .withUsername("eduvoyage")
            .withPassword("eduvoyage");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.elasticsearch.repositories.enabled", () -> "false");
        registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
    }

    @Autowired CourseService courseService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired DiscussionService discussionService;
    @Autowired NotificationService notificationService;
    @Autowired AnalyticsService analyticsService;

    private static final AuthUser TEACHER = new AuthUser(2L, "teacher", Set.of("TEACHER"),
            Set.of("course:create", "course:update", "discussion:write", "notification:write", "analytics:view"));
    private static final AuthUser STUDENT = new AuthUser(3L, "student", Set.of("STUDENT"),
            Set.of("course:enroll", "discussion:write", "notification:read", "analytics:view"));
    private static final AuthUser ADMIN = new AuthUser(1L, "admin", Set.of("ADMIN"),
            Set.of("analytics:view", "notification:write"));

    @Test
    void interactionNotificationAndAnalyticsFlow() {
        CourseResponse course = courseService.create(new CourseRequest("互动分析课程", null, "intro",
                new BigDecimal("2.0"), Course.VISIBILITY_PUBLIC, null, null, null), TEACHER.id()).block();
        assertThat(course).isNotNull();
        Long courseId = course.id();
        courseService.publish(courseId, TEACHER).block();
        enrollmentService.enroll(courseId, STUDENT.id()).block();

        var log = analyticsService.recordLog(new LearningLogRequest(courseId, null, "study_node", 120),
                STUDENT).block();
        assertThat(log).isNotNull();
        assertThat(log.action()).isEqualTo("STUDY_NODE");

        var post = discussionService.createPost(courseId,
                new PostRequest(null, "怎么复习？", "请问重点在哪里"), STUDENT).block();
        assertThat(post).isNotNull();

        StepVerifier.create(discussionService.reply(post.id(), new ReplyRequest("看知识图谱路径"), TEACHER))
                .assertNext(reply -> assertThat(reply.parentId()).isEqualTo(post.id()))
                .verifyComplete();

        StepVerifier.create(discussionService.toggleLike(post.id(), TEACHER))
                .assertNext(liked -> assertThat(liked.likeCount()).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(notificationService.announce(courseId,
                        new AnnouncementRequest("本周安排", "完成第一章"), TEACHER))
                .assertNext(result -> assertThat(result.recipientCount()).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(notificationService.unreadCount(STUDENT))
                .assertNext(count -> assertThat(count.unread()).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(notificationService.markAllRead(null, STUDENT))
                .assertNext(result -> assertThat(result.modifiedCount()).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(analyticsService.myDashboard(STUDENT))
                .assertNext(dashboard -> {
                    assertThat(dashboard.totalDurationSec()).isEqualTo(120);
                    assertThat(dashboard.activeDays()).isEqualTo(1);
                    assertThat(dashboard.enrolledCourses()).isEqualTo(1);
                })
                .verifyComplete();

        StepVerifier.create(analyticsService.courseDashboard(courseId, TEACHER))
                .assertNext(dashboard -> {
                    assertThat(dashboard.enrolledCount()).isEqualTo(1);
                    assertThat(dashboard.activeLearners()).isEqualTo(1);
                    assertThat(dashboard.totalDurationSec()).isEqualTo(120);
                })
                .verifyComplete();

        StepVerifier.create(analyticsService.adminDashboard(ADMIN))
                .assertNext(dashboard -> assertThat(dashboard.totalUsers()).isGreaterThanOrEqualTo(3))
                .verifyComplete();
    }
}
