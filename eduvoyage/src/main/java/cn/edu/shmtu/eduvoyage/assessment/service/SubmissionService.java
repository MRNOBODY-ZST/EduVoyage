package cn.edu.shmtu.eduvoyage.assessment.service;

import cn.edu.shmtu.eduvoyage.assessment.domain.Homework;
import cn.edu.shmtu.eduvoyage.assessment.domain.HomeworkQuestion;
import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import cn.edu.shmtu.eduvoyage.assessment.domain.Submission;
import cn.edu.shmtu.eduvoyage.assessment.domain.SubmissionAnswer;
import cn.edu.shmtu.eduvoyage.assessment.dto.GradingDtos.GradeItem;
import cn.edu.shmtu.eduvoyage.assessment.dto.GradingDtos.GradeRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.StudentOption;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.StudentQuestion;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.AnswerItem;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.AnswerResult;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.ExamPaper;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.SubmissionResult;
import cn.edu.shmtu.eduvoyage.assessment.dto.SubmissionDtos.SubmitRequest;
import cn.edu.shmtu.eduvoyage.assessment.grading.AnswerGrader;
import cn.edu.shmtu.eduvoyage.assessment.grading.AnswerGrader.Verdict;
import cn.edu.shmtu.eduvoyage.assessment.repository.HomeworkQuestionRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.HomeworkRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionOptionRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.SubmissionAnswerRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.SubmissionRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Submission flow: a student starts an attempt (reusing any in-progress one),
 * receives an answer-hidden paper, then submits. On submit, objective items are
 * auto-graded via {@link AnswerGrader}, the score is totaled from the paper's
 * per-question points, and every miss is pushed to the student's wrong book.
 * Subjective items stay pending until a teacher grades them.
 *
 * <p>Submission guards enforce the homework rules: published status, deadline and
 * {@code maxAttempts}. The {@link Clock} is injected so time-based rules are
 * deterministically testable.</p>
 */
@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final HomeworkRepository homeworkRepository;
    private final HomeworkQuestionRepository paperRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final WrongBookService wrongBookService;
    private final CourseService courseService;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public SubmissionService(SubmissionRepository submissionRepository,
                             SubmissionAnswerRepository answerRepository,
                             HomeworkRepository homeworkRepository,
                             HomeworkQuestionRepository paperRepository,
                             QuestionRepository questionRepository,
                             QuestionOptionRepository optionRepository,
                             WrongBookService wrongBookService,
                             CourseService courseService,
                             R2dbcEntityTemplate entityTemplate,
                             IdGenerator idGenerator,
                             Clock clock) {
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.homeworkRepository = homeworkRepository;
        this.paperRepository = paperRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.wrongBookService = wrongBookService;
        this.courseService = courseService;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    // -------------------------------------------------------------- start

    /**
     * Starts (or resumes) an attempt and returns the answer-hidden paper. Reuses an
     * existing in-progress submission so a refresh does not consume an attempt.
     */
    @Transactional
    public Mono<ExamPaper> start(Long homeworkId, Long studentId) {
        return requireOpenHomework(homeworkId)
                .flatMap(hw -> submissionRepository.findInProgress(homeworkId, studentId)
                        .switchIfEmpty(Mono.defer(() -> openNewAttempt(hw, studentId)))
                        .flatMap(submission -> buildPaper(hw, submission)));
    }

    private Mono<Submission> openNewAttempt(Homework hw, Long studentId) {
        int maxAttempts = hw.getMaxAttempts() == null ? 1 : hw.getMaxAttempts();
        return submissionRepository.countAttempts(hw.getId(), studentId)
                .flatMap(used -> {
                    if (used >= maxAttempts) {
                        return Mono.error(new BizException(BizErrorCode.SUBMISSION_LIMIT_EXCEEDED));
                    }
                    Submission submission = Submission.builder()
                            .id(idGenerator.nextId())
                            .homeworkId(hw.getId())
                            .studentId(studentId)
                            .attemptNo((int) (used + 1))
                            .status(Submission.STATUS_IN_PROGRESS)
                            .startedAt(now())
                            .switchCount(0)
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(Submission.class).using(submission);
                });
    }

    private Mono<ExamPaper> buildPaper(Homework hw, Submission submission) {
        return paperRepository.findByHomeworkId(hw.getId())
                .concatMap(item -> questionRepository.findActiveById(item.getQuestionId())
                        .flatMap(q -> optionRepository.findByQuestionId(q.getId())
                                .map(StudentOption::from)
                                .collectList()
                                .map(opts -> StudentQuestion.from(q, item.getScore(), opts))))
                .collectList()
                .map(questions -> new ExamPaper(submission.getId(), hw.getId(), hw.getTitle(),
                        submission.getAttemptNo(), hw.getTimeLimit(), hw.getDeadline(),
                        hw.getTotalScore(), questions));
    }

    // ------------------------------------------------------------- submit

    /**
     * Submits answers for an in-progress attempt: auto-grades objective items,
     * totals the score, records wrong answers and marks the submission submitted
     * (or graded if there were no subjective items).
     */
    @Transactional
    public Mono<SubmissionResult> submit(Long submissionId, SubmitRequest req, Long studentId) {
        return requireOwnedSubmission(submissionId, studentId)
                .flatMap(submission -> {
                    if (submission.getStatus() != null
                            && submission.getStatus() != Submission.STATUS_IN_PROGRESS) {
                        return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "该提交已结束"));
                    }
                    return requireOpenHomework(submission.getHomeworkId())
                            .flatMap(hw -> gradeAndPersist(hw, submission, req));
                });
    }

    private Mono<SubmissionResult> gradeAndPersist(Homework hw, Submission submission, SubmitRequest req) {
        Map<Long, String> answerByQuestion = new LinkedHashMap<>();
        for (AnswerItem item : req.answers()) {
            answerByQuestion.put(item.questionId(), item.answer());
        }

        return paperRepository.findByHomeworkId(hw.getId()).collectList()
                .flatMap(paperItems -> Flux.fromIterable(paperItems)
                        .concatMap(paperItem -> gradeOne(submission, paperItem,
                                answerByQuestion.get(paperItem.getQuestionId())))
                        .collectList()
                        .flatMap(graded -> finalize(hw, submission, req, graded)));
    }

    /** Grades a single paper item, persists the answer row and returns its grade. */
    private Mono<GradedAnswer> gradeOne(Submission submission, HomeworkQuestion paperItem, String studentAnswer) {
        return questionRepository.findActiveById(paperItem.getQuestionId())
                .flatMap(question -> {
                    Verdict verdict = AnswerGrader.grade(question.getType(),
                            question.getAnswer(), studentAnswer);
                    BigDecimal max = paperItem.getScore() == null ? BigDecimal.ZERO : paperItem.getScore();
                    BigDecimal score = verdict == Verdict.CORRECT ? max : BigDecimal.ZERO;
                    Integer isCorrect = switch (verdict) {
                        case CORRECT -> 1;
                        case WRONG -> 0;
                        case PENDING -> null;
                    };
                    SubmissionAnswer answer = SubmissionAnswer.builder()
                            .id(idGenerator.nextId())
                            .submissionId(submission.getId())
                            .questionId(question.getId())
                            .answer(studentAnswer)
                            .score(verdict == Verdict.PENDING ? null : score)
                            .isCorrect(isCorrect)
                            .build();
                    Mono<SubmissionAnswer> persisted =
                            entityTemplate.insert(SubmissionAnswer.class).using(answer);
                    // push objective misses to the wrong book
                    Mono<Void> wrongBook = verdict == Verdict.WRONG
                            ? wrongBookService.recordWrong(submission.getStudentId(), question)
                            : (verdict == Verdict.CORRECT
                                    ? wrongBookService.recordCorrect(submission.getStudentId(), question.getId())
                                    : Mono.empty());
                    return persisted.then(wrongBook)
                            .thenReturn(new GradedAnswer(question.getId(), studentAnswer, score, isCorrect,
                                    verdict == Verdict.PENDING));
                });
    }

    private Mono<SubmissionResult> finalize(Homework hw, Submission submission,
                                            SubmitRequest req, List<GradedAnswer> graded) {
        BigDecimal total = graded.stream()
                .map(g -> g.score == null ? BigDecimal.ZERO : g.score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasPending = graded.stream().anyMatch(g -> g.pending);

        submission.setStatus(hasPending ? Submission.STATUS_SUBMITTED : Submission.STATUS_GRADED);
        submission.setTotalScore(total);
        submission.setSubmittedAt(now());
        if (req.switchCount() != null) {
            submission.setSwitchCount(req.switchCount());
        }
        return submissionRepository.save(submission)
                .thenMany(answerRepository.findBySubmissionId(submission.getId()))
                .map(AnswerResult::from)
                .collectList()
                .map(answers -> SubmissionResult.from(submission, answers));
    }

    // ------------------------------------------------------- manual grade

    /** Teacher grades the (subjective) answers of a submission and finalizes it. */
    @Transactional
    public Mono<SubmissionResult> grade(Long submissionId, GradeRequest req, AuthUser grader) {
        return submissionRepository.findActiveById(submissionId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "提交不存在")))
                .flatMap(submission -> requireHomeworkEditable(submission.getHomeworkId(), grader)
                        .then(applyGrades(submission, req)));
    }

    private Mono<SubmissionResult> applyGrades(Submission submission, GradeRequest req) {
        Map<Long, GradeItem> byQuestion = new LinkedHashMap<>();
        for (GradeItem g : req.grades()) {
            byQuestion.put(g.questionId(), g);
        }
        return answerRepository.findBySubmissionId(submission.getId()).collectList()
                .flatMap(answers -> Flux.fromIterable(answers)
                        .concatMap(answer -> {
                            GradeItem g = byQuestion.get(answer.getQuestionId());
                            if (g == null) {
                                return Mono.just(answer);
                            }
                            return clampScore(submission.getHomeworkId(), answer.getQuestionId(), g.score())
                                    .flatMap(clamped -> {
                                        answer.setScore(clamped);
                                        answer.setComment(g.comment());
                                        return answerRepository.save(answer);
                                    });
                        })
                        .then(recomputeTotal(submission)));
    }

    /** Caps a manually-awarded score at the question's configured maximum. */
    private Mono<BigDecimal> clampScore(Long homeworkId, Long questionId, BigDecimal score) {
        return paperRepository.findByHomeworkId(homeworkId)
                .filter(item -> item.getQuestionId().equals(questionId))
                .next()
                .map(item -> {
                    BigDecimal max = item.getScore() == null ? BigDecimal.ZERO : item.getScore();
                    return score.compareTo(max) > 0 ? max : score;
                })
                .defaultIfEmpty(score);
    }

    private Mono<SubmissionResult> recomputeTotal(Submission submission) {
        return answerRepository.findBySubmissionId(submission.getId()).collectList()
                .flatMap(answers -> {
                    BigDecimal total = answers.stream()
                            .map(a -> a.getScore() == null ? BigDecimal.ZERO : a.getScore())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    submission.setStatus(Submission.STATUS_GRADED);
                    submission.setTotalScore(total);
                    return submissionRepository.save(submission)
                            .thenReturn(answers.stream().map(AnswerResult::from).toList())
                            .map(results -> SubmissionResult.from(submission, results));
                });
    }

    // -------------------------------------------------------------- reads

    public Mono<SubmissionResult> getResult(Long submissionId, Long studentId) {
        return requireOwnedSubmission(submissionId, studentId)
                .flatMap(submission -> answerRepository.findBySubmissionId(submissionId)
                        .map(AnswerResult::from)
                        .collectList()
                        .map(answers -> SubmissionResult.from(submission, answers)));
    }

    public Mono<List<SubmissionResult>> myAttempts(Long homeworkId, Long studentId) {
        return submissionRepository.findByHomeworkAndStudent(homeworkId, studentId)
                .flatMap(submission -> answerRepository.findBySubmissionId(submission.getId())
                        .map(AnswerResult::from)
                        .collectList()
                        .map(answers -> SubmissionResult.from(submission, answers)))
                .collectList();
    }

    public Mono<List<SubmissionResult>> listByHomework(Long homeworkId, AuthUser grader) {
        return requireHomeworkEditable(homeworkId, grader)
                .thenMany(Flux.defer(() -> submissionRepository.findByHomeworkId(homeworkId)))
                .flatMap(submission -> answerRepository.findBySubmissionId(submission.getId())
                        .map(AnswerResult::from)
                        .collectList()
                        .map(answers -> SubmissionResult.from(submission, answers)))
                .collectList();
    }

    // ------------------------------------------------------------ helpers

    private Mono<Homework> requireOpenHomework(Long homeworkId) {
        return homeworkRepository.findActiveById(homeworkId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "作业不存在")))
                .flatMap(hw -> {
                    if (hw.getStatus() == null || hw.getStatus() != Homework.STATUS_PUBLISHED) {
                        return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "作业未发布"));
                    }
                    if (hw.getDeadline() != null && now().isAfter(hw.getDeadline())) {
                        return Mono.error(new BizException(BizErrorCode.HOMEWORK_CLOSED));
                    }
                    return Mono.just(hw);
                });
    }

    private Mono<Submission> requireOwnedSubmission(Long submissionId, Long studentId) {
        return submissionRepository.findActiveById(submissionId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "提交不存在")))
                .flatMap(submission -> submission.getStudentId().equals(studentId)
                        ? Mono.just(submission)
                        : Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权访问该提交")));
    }

    private Mono<Void> requireHomeworkEditable(Long homeworkId, AuthUser editor) {
        return homeworkRepository.findActiveById(homeworkId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "作业不存在")))
                .flatMap(hw -> courseService.requireCourseEditable(hw.getCourseId(), editor))
                .then();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** Internal carrier for a graded answer during submit. */
    private record GradedAnswer(Long questionId, String answer, BigDecimal score,
                                Integer isCorrect, boolean pending) {
    }
}
