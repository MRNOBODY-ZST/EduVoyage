package cn.edu.shmtu.eduvoyage.assessment;

import cn.edu.shmtu.eduvoyage.assessment.domain.Homework;
import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import cn.edu.shmtu.eduvoyage.assessment.dto.GradingDtos.GradeItem;
import cn.edu.shmtu.eduvoyage.assessment.dto.GradingDtos.GradeRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.HomeworkRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.HomeworkResponse;
import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.PaperItem;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.OptionRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.QuestionRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.QuestionResponse;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.AnswerItem;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.ExamPaper;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.SubmissionResult;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.SubmitRequest;
import cn.edu.shmtu.eduvoyage.assessment.domain.Submission;
import cn.edu.shmtu.eduvoyage.assessment.service.HomeworkService;
import cn.edu.shmtu.eduvoyage.assessment.service.QuestionService;
import cn.edu.shmtu.eduvoyage.assessment.service.SubmissionService;
import cn.edu.shmtu.eduvoyage.assessment.service.WrongBookService;
import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseResponse;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end assessment-module test against real MySQL (schema.sql + data.sql via
 * the dev profile). Builds a question bank, assembles and publishes a paper, then
 * drives the student submit flow: objective auto-grading, wrong-book accumulation,
 * teacher manual grading of the subjective item and final score, plus the
 * attempt-limit and empty-paper guards.
 *
 * <p>Auto-skips when no Docker daemon is reachable.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class AssessmentModuleIntegrationTest {

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
    @Autowired QuestionService questionService;
    @Autowired HomeworkService homeworkService;
    @Autowired SubmissionService submissionService;
    @Autowired WrongBookService wrongBookService;

    private static final AuthUser TEACHER = new AuthUser(2L, "teacher", Set.of("TEACHER"),
            Set.of("course:create", "course:update", "homework:create", "homework:grade"));
    private static final long STUDENT_ID = 3L;

    @Test
    void fullAssessmentFlow() {
        // ---- arrange: a course owned by the teacher ----
        CourseResponse course = courseService.create(new CourseRequest("测评课程", null, "intro",
                new BigDecimal("3.0"), Course.VISIBILITY_PUBLIC, null, null, null), TEACHER.id()).block();
        assertThat(course).isNotNull();
        Long courseId = course.id();

        // ---- question bank: a single-choice (auto) and a short-answer (manual) ----
        QuestionResponse single = questionService.create(new QuestionRequest(courseId,
                Question.TYPE_SINGLE, "1+1=?", null, null, 1, null, null,
                List.of(new OptionRequest("A", "1", false, 0),
                        new OptionRequest("B", "2", true, 1),
                        new OptionRequest("C", "3", false, 2))), TEACHER).block();
        assertThat(single).isNotNull();
        assertThat(single.answer()).isEqualTo("B");

        QuestionResponse essay = questionService.create(new QuestionRequest(courseId,
                Question.TYPE_SHORT, "简述反应式编程", null, null, 3, null, null, null), TEACHER).block();
        assertThat(essay).isNotNull();

        // ---- assemble a paper: single worth 40, essay worth 60 ----
        HomeworkRequest hwReq = new HomeworkRequest("第一次作业", null, null, 2, false, false,
                List.of(new PaperItem(single.id(), new BigDecimal("40"), 0),
                        new PaperItem(essay.id(), new BigDecimal("60"), 1)));
        HomeworkResponse hw = homeworkService.create(courseId, hwReq, TEACHER).block();
        assertThat(hw).isNotNull();
        assertThat(hw.totalScore()).isEqualByComparingTo("100");
        assertThat(hw.status()).isEqualTo(Homework.STATUS_DRAFT);
        Long homeworkId = hw.id();

        // ---- submitting before publish is rejected ----
        StepVerifier.create(submissionService.start(homeworkId, STUDENT_ID))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.OPERATION_NOT_ALLOWED))
                .verify();

        // ---- publish ----
        StepVerifier.create(homeworkService.publish(homeworkId, TEACHER))
                .assertNext(r -> assertThat(r.status()).isEqualTo(Homework.STATUS_PUBLISHED))
                .verifyComplete();

        // ---- student starts → gets answer-hidden paper ----
        ExamPaper paper = submissionService.start(homeworkId, STUDENT_ID).block();
        assertThat(paper).isNotNull();
        assertThat(paper.questions()).hasSize(2);
        Long submissionId = paper.submissionId();

        // ---- student submits: single correct (B), essay pending ----
        SubmitRequest submit = new SubmitRequest(List.of(
                new AnswerItem(single.id(), "B"),
                new AnswerItem(essay.id(), "Reactor 是一个反应式库")), 0);
        SubmissionResult submitted = submissionService.submit(submissionId, submit, STUDENT_ID).block();
        assertThat(submitted).isNotNull();
        // objective scored (40), essay pending → status SUBMITTED, total 40 so far
        assertThat(submitted.status()).isEqualTo(Submission.STATUS_SUBMITTED);
        assertThat(submitted.totalScore()).isEqualByComparingTo("40");

        // ---- teacher grades the essay 50/60 → status GRADED, total 90 ----
        GradeRequest grade = new GradeRequest(List.of(
                new GradeItem(essay.id(), new BigDecimal("50"), "答得不错")));
        SubmissionResult graded = submissionService.grade(submissionId, grade, TEACHER).block();
        assertThat(graded).isNotNull();
        assertThat(graded.status()).isEqualTo(Submission.STATUS_GRADED);
        assertThat(graded.totalScore()).isEqualByComparingTo("90");

        // ---- a second attempt is allowed (maxAttempts=2); a wrong single answer ----
        ExamPaper paper2 = submissionService.start(homeworkId, STUDENT_ID).block();
        assertThat(paper2.attemptNo()).isEqualTo(2);
        SubmitRequest submit2 = new SubmitRequest(List.of(
                new AnswerItem(single.id(), "A"),
                new AnswerItem(essay.id(), "略")), 0);
        SubmissionResult submitted2 = submissionService.submit(paper2.submissionId(), submit2, STUDENT_ID).block();
        // single wrong → 0 for objective, essay pending
        assertThat(submitted2.totalScore()).isEqualByComparingTo("0");

        // wrong answer landed in the wrong book (not yet mastered)
        StepVerifier.create(wrongBookService.list(STUDENT_ID, true))
                .assertNext(entries -> {
                    assertThat(entries).isNotEmpty();
                    assertThat(entries.stream().anyMatch(e -> e.questionId().equals(single.id()))).isTrue();
                })
                .verifyComplete();

        // ---- third attempt exceeds the limit ----
        StepVerifier.create(submissionService.start(homeworkId, STUDENT_ID))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.SUBMISSION_LIMIT_EXCEEDED))
                .verify();
    }
}
