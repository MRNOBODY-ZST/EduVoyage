package cn.edu.shmtu.eduvoyage.assessment.service;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import cn.edu.shmtu.eduvoyage.assessment.domain.QuestionOption;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.OptionRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.OptionView;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.QuestionRequest;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.QuestionResponse;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionOptionRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionQueryRepository;
import cn.edu.shmtu.eduvoyage.assessment.repository.QuestionRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Question-bank authoring: dynamic search, detail, create/update/delete with the
 * option set, plus reference-answer derivation. A question may belong to a course
 * (course-scoped bank) or be global ({@code courseId == null}); course-scoped
 * mutation requires the caller to own the course, while global items require ADMIN.
 *
 * <p>For choice/judge questions the canonical {@code answer} key string is derived
 * from the options' {@code correct} flags so the stored answer and the option set
 * can never drift apart.</p>
 */
@Service
public class QuestionService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuestionQueryRepository queryRepository;
    private final CourseService courseService;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public QuestionService(QuestionRepository questionRepository,
                           QuestionOptionRepository optionRepository,
                           QuestionQueryRepository queryRepository,
                           CourseService courseService,
                           R2dbcEntityTemplate entityTemplate,
                           IdGenerator idGenerator) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.queryRepository = queryRepository;
        this.courseService = courseService;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    // ------------------------------------------------------------- queries

    public Mono<PageResult<QuestionResponse>> page(Long courseId, String keyword, Integer type,
                                                   Integer difficulty, Long nodeId, int pageNo, int pageSize) {
        int safeNo = Math.max(1, pageNo);
        int safeSize = Math.min(Math.max(1, pageSize), 200);
        int offset = (safeNo - 1) * safeSize;

        Mono<List<QuestionResponse>> rows = queryRepository
                .search(courseId, keyword, type, difficulty, nodeId, offset, safeSize)
                .flatMap(q -> loadOptions(q.getId())
                        .map(opts -> QuestionResponse.from(q, opts)))
                .collectList();
        Mono<Long> total = queryRepository.count(courseId, keyword, type, difficulty, nodeId);

        return Mono.zip(rows, total)
                .map(t -> PageResult.of(t.getT1(), t.getT2(), safeNo, safeSize));
    }

    public Mono<QuestionResponse> get(Long id) {
        return requireQuestion(id)
                .flatMap(q -> loadOptions(id).map(opts -> QuestionResponse.from(q, opts)));
    }

    // -------------------------------------------------------------- create

    @Transactional
    public Mono<QuestionResponse> create(QuestionRequest req, AuthUser editor) {
        BizException invalid = validateOptions(req);
        if (invalid != null) {
            return Mono.error(invalid);
        }
        return authorizeBankAccess(req.courseId(), editor)
                .then(Mono.defer(() -> {
                    Question question = Question.builder()
                            .id(idGenerator.nextId())
                            .courseId(req.courseId())
                            .type(req.type())
                            .stem(req.stem())
                            .answer(resolveAnswer(req))
                            .analysis(req.analysis())
                            .difficulty(req.difficulty() == null ? 1 : req.difficulty())
                            .nodeId(req.nodeId())
                            .lang(req.lang())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(Question.class).using(question)
                            .flatMap(saved -> saveOptions(saved.getId(), req)
                                    .then(loadOptions(saved.getId()))
                                    .map(opts -> QuestionResponse.from(saved, opts)));
                }));
    }

    @Transactional
    public Mono<QuestionResponse> update(Long id, QuestionRequest req, AuthUser editor) {
        BizException invalid = validateOptions(req);
        if (invalid != null) {
            return Mono.error(invalid);
        }
        return requireQuestion(id)
                .flatMap(q -> authorizeBankAccess(q.getCourseId(), editor).thenReturn(q))
                .flatMap(q -> {
                    q.setType(req.type());
                    q.setStem(req.stem());
                    q.setAnswer(resolveAnswer(req));
                    q.setAnalysis(req.analysis());
                    if (req.difficulty() != null) {
                        q.setDifficulty(req.difficulty());
                    }
                    q.setNodeId(req.nodeId());
                    q.setLang(req.lang());
                    // replace the option set wholesale (options have no soft-delete)
                    return optionRepository.deleteByQuestionId(id)
                            .then(questionRepository.save(q))
                            .then(saveOptions(id, req))
                            .then(loadOptions(id))
                            .map(opts -> QuestionResponse.from(q, opts));
                });
    }

    @Transactional
    public Mono<Void> delete(Long id, AuthUser editor) {
        return requireQuestion(id)
                .flatMap(q -> authorizeBankAccess(q.getCourseId(), editor).thenReturn(q))
                .flatMap(q -> {
                    q.setDeleted(1);
                    return questionRepository.save(q);
                })
                .then();
    }

    // ------------------------------------------------------------ helpers

    Mono<Question> requireQuestion(Long id) {
        return questionRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "题目不存在")));
    }

    Mono<List<OptionView>> loadOptions(Long questionId) {
        return optionRepository.findByQuestionId(questionId)
                .map(OptionView::from)
                .collectList();
    }

    /**
     * Course-scoped questions require course-edit rights; global questions
     * ({@code courseId == null}) require ADMIN.
     */
    private Mono<Void> authorizeBankAccess(Long courseId, AuthUser editor) {
        if (courseId == null) {
            return editor != null && editor.hasRole(ROLE_ADMIN)
                    ? Mono.empty()
                    : Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "仅管理员可维护公共题库"));
        }
        return courseService.requireCourseEditable(courseId, editor).then();
    }

    private Mono<Void> saveOptions(Long questionId, QuestionRequest req) {
        if (!hasOptions(req)) {
            return Mono.empty();
        }
        int[] order = {0};
        return Flux.fromIterable(req.options())
                .concatMap(opt -> {
                    QuestionOption option = QuestionOption.builder()
                            .id(idGenerator.nextId())
                            .questionId(questionId)
                            .optionKey(opt.optionKey())
                            .content(opt.content())
                            .isCorrect(opt.correct() ? 1 : 0)
                            .sortNo(opt.sortNo() == null ? order[0]++ : opt.sortNo())
                            .build();
                    return entityTemplate.insert(QuestionOption.class).using(option);
                })
                .then();
    }

    /** For choice/judge types the answer is the sorted set of correct option keys. */
    private static String resolveAnswer(QuestionRequest req) {
        if (hasOptions(req) && (req.type() == Question.TYPE_SINGLE
                || req.type() == Question.TYPE_MULTIPLE
                || req.type() == Question.TYPE_JUDGE)) {
            return req.options().stream()
                    .filter(OptionRequest::correct)
                    .map(OptionRequest::optionKey)
                    .map(String::trim)
                    .sorted()
                    .collect(Collectors.joining(","));
        }
        return req.answer();
    }

    private static boolean hasOptions(QuestionRequest req) {
        return req.options() != null && !req.options().isEmpty();
    }

    /** Choice/judge questions must carry options with at least one correct key. */
    private static BizException validateOptions(QuestionRequest req) {
        boolean needsOptions = req.type() == Question.TYPE_SINGLE
                || req.type() == Question.TYPE_MULTIPLE
                || req.type() == Question.TYPE_JUDGE;
        if (!needsOptions) {
            return null;
        }
        if (!hasOptions(req)) {
            return new BizException(BizErrorCode.PARAM_INVALID, "选择/判断题必须提供选项");
        }
        long correct = req.options().stream().filter(OptionRequest::correct).count();
        if (correct == 0) {
            return new BizException(BizErrorCode.PARAM_INVALID, "至少需要一个正确选项");
        }
        if (req.type() == Question.TYPE_SINGLE && correct != 1) {
            return new BizException(BizErrorCode.PARAM_INVALID, "单选题只能有一个正确选项");
        }
        return null;
    }
}
