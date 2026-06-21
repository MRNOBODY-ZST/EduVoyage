package cn.edu.shmtu.eduvoyage.assessment.service;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import cn.edu.shmtu.eduvoyage.assessment.domain.QuestionOption;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.OptionRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.QuestionRequest;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionOptionRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionQueryRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionRepository;
import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuestionService} option validation, canonical-answer
 * derivation and global-bank authorization with mocked collaborators.
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository optionRepository;
    @Mock QuestionQueryRepository queryRepository;
    @Mock CourseService courseService;
    @Mock R2dbcEntityTemplate entityTemplate;
    @Mock ReactiveInsert<Question> questionInsert;
    @Mock ReactiveInsert<QuestionOption> optionInsert;

    private final IdGenerator idGenerator = new IdGenerator(1L);
    private QuestionService service;

    private static final long COURSE_ID = 900L;
    private static final AuthUser TEACHER =
            new AuthUser(200L, "t", Set.of("TEACHER"), Set.of("homework:create"));
    private static final AuthUser STUDENT =
            new AuthUser(300L, "s", Set.of("STUDENT"), Set.of());

    @BeforeEach
    void setUp() {
        service = new QuestionService(questionRepository, optionRepository, queryRepository,
                courseService, entityTemplate, idGenerator);
    }

    private static OptionRequest opt(String key, String content, boolean correct) {
        return new OptionRequest(key, content, correct, null);
    }

    @Test
    void singleChoiceWithoutCorrectOptionIsRejected() {
        QuestionRequest req = new QuestionRequest(COURSE_ID, Question.TYPE_SINGLE, "stem", null, null,
                1, null, null, List.of(opt("A", "x", false), opt("B", "y", false)));

        StepVerifier.create(service.create(req, TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.PARAM_INVALID))
                .verify();
    }

    @Test
    void singleChoiceWithMultipleCorrectIsRejected() {
        QuestionRequest req = new QuestionRequest(COURSE_ID, Question.TYPE_SINGLE, "stem", null, null,
                1, null, null, List.of(opt("A", "x", true), opt("B", "y", true)));

        StepVerifier.create(service.create(req, TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.PARAM_INVALID))
                .verify();
    }

    @Test
    void createDerivesCanonicalAnswerFromCorrectOptions() {
        QuestionRequest req = new QuestionRequest(COURSE_ID, Question.TYPE_MULTIPLE, "stem", null, null,
                2, null, null, List.of(opt("C", "z", true), opt("A", "x", true), opt("B", "y", false)));

        when(courseService.requireCourseEditable(COURSE_ID, TEACHER))
                .thenReturn(Mono.just(Course.builder().id(COURSE_ID).teacherId(TEACHER.id()).build()));
        when(entityTemplate.insert(Question.class)).thenReturn(questionInsert);
        when(questionInsert.using(any(Question.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(entityTemplate.insert(QuestionOption.class)).thenReturn(optionInsert);
        when(optionInsert.using(any(QuestionOption.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(optionRepository.findByQuestionId(any())).thenReturn(Flux.empty());

        StepVerifier.create(service.create(req, TEACHER))
                .assertNext(resp -> {
                    // correct keys sorted and comma-joined: A,C
                    assertThat(resp.answer()).isEqualTo("A,C");
                    assertThat(resp.type()).isEqualTo(Question.TYPE_MULTIPLE);
                })
                .verifyComplete();
    }

    @Test
    void globalBankRequiresAdmin() {
        // courseId == null => global bank, a teacher (non-admin) is denied
        QuestionRequest req = new QuestionRequest(null, Question.TYPE_FILL, "stem", "ans", null,
                1, null, null, null);

        StepVerifier.create(service.create(req, TEACHER))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ACCESS_DENIED))
                .verify();
    }
}
