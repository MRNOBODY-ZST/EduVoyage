package cn.edu.shmtu.eduvoyage.course.service;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeGraph;
import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.repository.CourseQueryRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRelationRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeGraphRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation.ReactiveInsert;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CourseService} ownership, status and validation logic
 * with mocked collaborators. The reactive contracts are asserted with
 * {@link StepVerifier}; no Spring context or database is involved.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock CourseQueryRepository courseQueryRepository;
    @Mock CourseRelationRepository relationRepository;
    @Mock KnowledgeGraphRepository graphRepository;
    @Mock R2dbcEntityTemplate entityTemplate;
    @Mock ReactiveInsert<Course> courseInsert;
    @Mock ReactiveInsert<KnowledgeGraph> graphInsert;

    private final IdGenerator idGenerator = new IdGenerator(1L);
    private CourseService courseService;

    private static final long TEACHER_ID = 200L;
    private static final long OTHER_TEACHER_ID = 201L;

    private static AuthUser teacher(long id) {
        return new AuthUser(id, "t" + id, Set.of("TEACHER"), Set.of("course:create", "course:update"));
    }

    private static AuthUser admin() {
        return new AuthUser(1L, "admin", Set.of("ADMIN"), Set.of("course:update"));
    }

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, courseQueryRepository, relationRepository,
                graphRepository, entityTemplate, idGenerator);
    }

    private Course publishedCourseOwnedBy(long teacherId) {
        return Course.builder()
                .id(900L)
                .title("数据结构")
                .teacherId(teacherId)
                .visibility(Course.VISIBILITY_PRIVATE)
                .status(Course.STATUS_PUBLISHED)
                .deleted(0)
                .build();
    }

    @Test
    void createProvisionsDefaultGraphAndStartsAsDraft() {
        CourseRequest req = new CourseRequest("算法导论", null, "intro", null,
                Course.VISIBILITY_PUBLIC, null, null, null);

        // entityTemplate.insert(Course.class).using(course) -> echoes the entity
        when(entityTemplate.insert(Course.class)).thenReturn(courseInsert);
        when(courseInsert.using(any(Course.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(entityTemplate.insert(KnowledgeGraph.class)).thenReturn(graphInsert);
        when(graphInsert.using(any(KnowledgeGraph.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(courseService.create(req, TEACHER_ID))
                .assertNext(resp -> {
                    assertThat(resp.teacherId()).isEqualTo(TEACHER_ID);
                    assertThat(resp.status()).isEqualTo(Course.STATUS_DRAFT);
                    assertThat(resp.visibility()).isEqualTo(Course.VISIBILITY_PUBLIC);
                    assertThat(resp.title()).isEqualTo("算法导论");
                })
                .verifyComplete();
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        CourseRequest req = new CourseRequest("课程", null, null, null, null,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1), null);

        StepVerifier.create(courseService.create(req, TEACHER_ID))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.PARAM_INVALID))
                .verify();
    }

    @Test
    void publishByOwningTeacherSetsPublishedStatus() {
        Course course = Course.builder()
                .id(900L).title("c").teacherId(TEACHER_ID)
                .status(Course.STATUS_DRAFT).deleted(0).build();
        when(courseRepository.findActiveById(900L)).thenReturn(Mono.just(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(courseService.publish(900L, teacher(TEACHER_ID)))
                .assertNext(resp -> assertThat(resp.status()).isEqualTo(Course.STATUS_PUBLISHED))
                .verifyComplete();
    }

    @Test
    void publishByForeignTeacherIsDenied() {
        Course course = publishedCourseOwnedBy(TEACHER_ID);
        when(courseRepository.findActiveById(900L)).thenReturn(Mono.just(course));
        // not owner, not admin -> falls back to co-teacher check, which is false
        when(relationRepository.isCourseTeacher(900L, OTHER_TEACHER_ID)).thenReturn(Mono.just(false));

        StepVerifier.create(courseService.publish(900L, teacher(OTHER_TEACHER_ID)))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ACCESS_DENIED))
                .verify();
    }

    @Test
    void adminMayArchiveAnyCourse() {
        Course course = publishedCourseOwnedBy(TEACHER_ID);
        when(courseRepository.findActiveById(900L)).thenReturn(Mono.just(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        // admin shortcut means the co-teacher query must not be needed
        lenient().when(relationRepository.isCourseTeacher(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(courseService.archive(900L, admin()))
                .assertNext(resp -> assertThat(resp.status()).isEqualTo(Course.STATUS_ARCHIVED))
                .verifyComplete();
    }

    @Test
    void updateMissingCourseReturnsNotFound() {
        CourseRequest req = new CourseRequest("x", null, null, null, null, null, null, null);
        when(courseRepository.findActiveById(404L)).thenReturn(Mono.empty());

        StepVerifier.create(courseService.update(404L, req, teacher(TEACHER_ID)))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.RESOURCE_NOT_FOUND))
                .verify();
    }
}
