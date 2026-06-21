package cn.edu.shmtu.eduvoyage.course;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.dto.ChapterDtos.ChapterNode;
import cn.edu.shmtu.eduvoyage.course.dto.ChapterDtos.ChapterRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseResponse;
import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeRequest;
import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeResponse;
import cn.edu.shmtu.eduvoyage.course.service.ChapterService;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.course.service.EnrollmentService;
import cn.edu.shmtu.eduvoyage.course.service.KnowledgeNodeService;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
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

/**
 * End-to-end course-module test against real MySQL (schema.sql + data.sql via the
 * dev profile). Exercises the wired services across the full authoring →
 * publish → enroll flow, including the default-graph provisioning, chapter tree,
 * knowledge-point creation, ownership enforcement and student enrollment rules.
 *
 * <p>Auto-skips when no Docker daemon is reachable.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class CourseModuleIntegrationTest {

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
    @Autowired ChapterService chapterService;
    @Autowired KnowledgeNodeService nodeService;
    @Autowired EnrollmentService enrollmentService;

    // seeded accounts (data.sql): teacher id=2, student id=3
    private static final AuthUser TEACHER = new AuthUser(2L, "teacher",
            Set.of("TEACHER"), Set.of("course:create", "course:update"));
    private static final AuthUser OTHER_TEACHER = new AuthUser(999L, "ghost",
            Set.of("TEACHER"), Set.of("course:update"));
    private static final long STUDENT_ID = 3L;

    @Test
    void fullAuthoringAndEnrollmentFlow() {
        // 1. create a course (draft) → default graph provisioned
        CourseRequest courseReq = new CourseRequest("集成测试课程", null, "intro",
                new BigDecimal("3.0"), Course.VISIBILITY_PUBLIC, null, null, null);
        CourseResponse created = courseService.create(courseReq, TEACHER.id()).block();
        assertThat(created).isNotNull();
        assertThat(created.status()).isEqualTo(Course.STATUS_DRAFT);
        Long courseId = created.id();

        // 2. add a chapter
        ChapterNode chapter = chapterService.create(courseId,
                new ChapterRequest("第一章", null, 1), TEACHER).block();
        assertThat(chapter).isNotNull();

        // 3. add a knowledge point under the chapter (uses the auto graph)
        NodeResponse node = nodeService.create(courseId,
                new NodeRequest("知识点A", chapter.id(), "desc", "目标", 30, 1.0, 2.0), TEACHER).block();
        assertThat(node).isNotNull();
        assertThat(node.graphId()).isNotNull();
        assertThat(node.chapterId()).isEqualTo(chapter.id());

        // 4. chapter tree reflects the new chapter
        StepVerifier.create(chapterService.tree(courseId))
                .assertNext(tree -> {
                    assertThat(tree).hasSize(1);
                    assertThat(tree.get(0).title()).isEqualTo("第一章");
                })
                .verifyComplete();

        // 5. a foreign teacher cannot publish it
        StepVerifier.create(courseService.publish(courseId, OTHER_TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ACCESS_DENIED))
                .verify();

        // 6. enrollment is rejected while the course is still a draft
        StepVerifier.create(enrollmentService.enroll(courseId, STUDENT_ID))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.OPERATION_NOT_ALLOWED))
                .verify();

        // 7. owning teacher publishes
        StepVerifier.create(courseService.publish(courseId, TEACHER))
                .assertNext(resp -> assertThat(resp.status()).isEqualTo(Course.STATUS_PUBLISHED))
                .verifyComplete();

        // 8. student enrolls, updates progress; re-enroll is a conflict
        StepVerifier.create(enrollmentService.enroll(courseId, STUDENT_ID))
                .assertNext(e -> assertThat(e.status()).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(enrollmentService.updateProgress(courseId, STUDENT_ID, new BigDecimal("55.5")))
                .assertNext(e -> assertThat(e.progress()).isEqualByComparingTo("55.5"))
                .verifyComplete();

        StepVerifier.create(enrollmentService.enroll(courseId, STUDENT_ID))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.DATA_CONFLICT))
                .verify();

        // 9. drop then re-enroll reactivates the same record (no duplicate)
        enrollmentService.drop(courseId, STUDENT_ID).block();
        StepVerifier.create(enrollmentService.enroll(courseId, STUDENT_ID))
                .assertNext(e -> assertThat(e.status()).isEqualTo(1))
                .verifyComplete();
    }
}
