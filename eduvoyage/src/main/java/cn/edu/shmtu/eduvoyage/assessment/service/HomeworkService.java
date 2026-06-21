package cn.edu.shmtu.eduvoyage.assessment.service;

import cn.edu.shmtu.eduvoyage.assessment.domain.Homework;
import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.HomeworkRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.HomeworkResponse;
import cn.edu.shmtu.eduvoyage.assessment.dto.HomeworkDtos.PaperItem;
import cn.edu.shmtu.eduvoyage.assessment.repository.HomeworkQuestionRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.HomeworkRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionRepository;
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
import java.util.List;

/**
 * Homework (paper) lifecycle: list, detail, create/update from a set of
 * question-bank items, publish/close and delete. The paper's total score is the
 * sum of its item scores, recomputed on every edit. Authoring authorization is
 * delegated to {@link CourseService#requireCourseEditable}. Editing the question
 * set is only allowed while the paper is a draft, so a published exam is stable.
 */
@Service
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final HomeworkQuestionRepository paperRepository;
    private final QuestionRepository questionRepository;
    private final CourseService courseService;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public HomeworkService(HomeworkRepository homeworkRepository,
                           HomeworkQuestionRepository paperRepository,
                           QuestionRepository questionRepository,
                           CourseService courseService,
                           R2dbcEntityTemplate entityTemplate,
                           IdGenerator idGenerator) {
        this.homeworkRepository = homeworkRepository;
        this.paperRepository = paperRepository;
        this.questionRepository = questionRepository;
        this.courseService = courseService;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    // ------------------------------------------------------------- queries

    public Mono<List<HomeworkResponse>> listByCourse(Long courseId) {
        return homeworkRepository.findByCourseId(courseId)
                .flatMap(this::toResponse)
                .collectList();
    }

    public Mono<HomeworkResponse> get(Long id) {
        return requireHomework(id).flatMap(this::toResponse);
    }

    // -------------------------------------------------------- create/update

    @Transactional
    public Mono<HomeworkResponse> create(Long courseId, HomeworkRequest req, AuthUser editor) {
        return courseService.requireCourseEditable(courseId, editor)
                .then(validateItems(req.items()))
                .then(Mono.defer(() -> {
                    Homework homework = Homework.builder()
                            .id(idGenerator.nextId())
                            .courseId(courseId)
                            .title(req.title())
                            .totalScore(sumItems(req.items()))
                            .timeLimit(req.timeLimit())
                            .deadline(req.deadline())
                            .maxAttempts(req.maxAttempts() == null ? 1 : req.maxAttempts())
                            .shuffle(req.shuffle() ? 1 : 0)
                            .antiSwitch(req.antiSwitch() ? 1 : 0)
                            .status(Homework.STATUS_DRAFT)
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(Homework.class).using(homework)
                            .flatMap(saved -> replaceItems(saved.getId(), req.items())
                                    .then(toResponse(saved)));
                }));
    }

    @Transactional
    public Mono<HomeworkResponse> update(Long id, HomeworkRequest req, AuthUser editor) {
        return requireEditableHomework(id, editor)
                .flatMap(h -> {
                    if (h.getStatus() != null && h.getStatus() != Homework.STATUS_DRAFT) {
                        return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED,
                                "已发布的作业不可修改题目，请先关闭"));
                    }
                    return validateItems(req.items()).thenReturn(h);
                })
                .flatMap(h -> {
                    h.setTitle(req.title());
                    h.setTotalScore(sumItems(req.items()));
                    h.setTimeLimit(req.timeLimit());
                    h.setDeadline(req.deadline());
                    if (req.maxAttempts() != null) {
                        h.setMaxAttempts(req.maxAttempts());
                    }
                    h.setShuffle(req.shuffle() ? 1 : 0);
                    h.setAntiSwitch(req.antiSwitch() ? 1 : 0);
                    return homeworkRepository.save(h)
                            .then(replaceItems(id, req.items()))
                            .then(toResponse(h));
                });
    }

    // ------------------------------------------------------ status changes

    public Mono<HomeworkResponse> publish(Long id, AuthUser editor) {
        return transition(id, editor, Homework.STATUS_PUBLISHED, true);
    }

    public Mono<HomeworkResponse> close(Long id, AuthUser editor) {
        return transition(id, editor, Homework.STATUS_CLOSED, false);
    }

    private Mono<HomeworkResponse> transition(Long id, AuthUser editor, int status, boolean requireQuestions) {
        return requireEditableHomework(id, editor)
                .flatMap(h -> paperRepository.findByHomeworkId(id).count()
                        .flatMap(count -> {
                            if (requireQuestions && count == 0) {
                                return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED,
                                        "空试卷不能发布"));
                            }
                            h.setStatus(status);
                            return homeworkRepository.save(h);
                        }))
                .flatMap(this::toResponse);
    }

    @Transactional
    public Mono<Void> delete(Long id, AuthUser editor) {
        return requireEditableHomework(id, editor)
                .flatMap(h -> {
                    h.setDeleted(1);
                    return homeworkRepository.save(h);
                })
                .then();
    }

    // ------------------------------------------------------------ helpers

    Mono<Homework> requireHomework(Long id) {
        return homeworkRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "作业不存在")));
    }

    private Mono<Homework> requireEditableHomework(Long id, AuthUser editor) {
        return requireHomework(id)
                .flatMap(h -> courseService.requireCourseEditable(h.getCourseId(), editor).thenReturn(h));
    }

    private Mono<HomeworkResponse> toResponse(Homework h) {
        return paperRepository.findByHomeworkId(h.getId())
                .map(item -> new PaperItem(item.getQuestionId(), item.getScore(), item.getSortNo()))
                .collectList()
                .map(items -> HomeworkResponse.from(h, items));
    }

    private Mono<Void> replaceItems(Long homeworkId, List<PaperItem> items) {
        Mono<Void> clear = paperRepository.deleteByHomeworkId(homeworkId);
        if (items == null || items.isEmpty()) {
            return clear;
        }
        int[] order = {0};
        return clear.thenMany(Flux.fromIterable(items)
                        .concatMap(item -> paperRepository.add(homeworkId, item.questionId(),
                                item.score(), item.sortNo() == null ? order[0]++ : item.sortNo())))
                .then();
    }

    /** Every referenced question must exist (active). */
    private Mono<Void> validateItems(List<PaperItem> items) {
        if (items == null || items.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(items)
                .concatMap(item -> questionRepository.findActiveById(item.questionId())
                        .switchIfEmpty(Mono.error(new BizException(BizErrorCode.PARAM_INVALID,
                                "题目不存在: " + item.questionId()))))
                .then();
    }

    private static BigDecimal sumItems(List<PaperItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream().map(PaperItem::score).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
