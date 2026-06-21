package cn.edu.shmtu.eduvoyage.course.service;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeGraph;
import cn.edu.shmtu.eduvoyage.course.dto.CourseRequest;
import cn.edu.shmtu.eduvoyage.course.dto.CourseResponse;
import cn.edu.shmtu.eduvoyage.course.repository.CourseQueryRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRelationRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeGraphRepository;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Course lifecycle: catalog search, detail, create/update/delete, publish and
 * archive. Creating a course also provisions its default {@link KnowledgeGraph}
 * so knowledge points always have a home. Authorization: only the owning teacher
 * (or a co-teacher), or an ADMIN, may mutate a course — enforced here in addition
 * to the coarse {@code course:*} permission checks at the controller.
 */
@Service
public class CourseService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final CourseRepository courseRepository;
    private final CourseQueryRepository courseQueryRepository;
    private final CourseRelationRepository relationRepository;
    private final KnowledgeGraphRepository graphRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public CourseService(CourseRepository courseRepository,
                         CourseQueryRepository courseQueryRepository,
                         CourseRelationRepository relationRepository,
                         KnowledgeGraphRepository graphRepository,
                         R2dbcEntityTemplate entityTemplate,
                         IdGenerator idGenerator) {
        this.courseRepository = courseRepository;
        this.courseQueryRepository = courseQueryRepository;
        this.relationRepository = relationRepository;
        this.graphRepository = graphRepository;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    // ------------------------------------------------------------- queries

    public Mono<PageResult<CourseResponse>> page(String keyword, Long teacherId, Integer status,
                                                 Integer visibility, int pageNo, int pageSize) {
        int safeNo = Math.max(1, pageNo);
        int safeSize = Math.min(Math.max(1, pageSize), 200);
        int offset = (safeNo - 1) * safeSize;

        Mono<List<CourseResponse>> rows = courseQueryRepository
                .search(keyword, teacherId, status, visibility, offset, safeSize)
                .map(CourseResponse::from)
                .collectList();
        Mono<Long> total = courseQueryRepository.count(keyword, teacherId, status, visibility);

        return Mono.zip(rows, total)
                .map(t -> PageResult.of(t.getT1(), t.getT2(), safeNo, safeSize));
    }

    /** Full detail including class scope and (for a student) enrolled/favorite flags. */
    public Mono<CourseResponse> get(Long id, AuthUser viewer) {
        return requireCourse(id)
                .flatMap(course -> {
                    Mono<List<Long>> scope = relationRepository.findScopeClassIds(id).collectList();
                    Long viewerId = viewer == null ? null : viewer.id();
                    Mono<Boolean> favorite = viewerId == null ? Mono.just(false)
                            : relationRepository.isFavorite(viewerId, id);
                    return Mono.zip(scope, favorite)
                            .map(t -> CourseResponse.from(course, t.getT1(), null, t.getT2()));
                });
    }

    // -------------------------------------------------------------- create

    @Transactional
    public Mono<CourseResponse> create(CourseRequest req, Long teacherId) {
        BizException dateError = validateDates(req);
        if (dateError != null) {
            return Mono.error(dateError);
        }
        Course course = Course.builder()
                .id(idGenerator.nextId())
                .title(req.title())
                .coverUrl(req.coverUrl())
                .intro(req.intro())
                .credit(req.credit() == null ? BigDecimal.ZERO : req.credit())
                .teacherId(teacherId)
                .visibility(req.visibility() == null ? Course.VISIBILITY_PRIVATE : req.visibility())
                .status(Course.STATUS_DRAFT)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .deleted(0)
                .build();

        return entityTemplate.insert(Course.class).using(course)
                .flatMap(saved -> provisionDefaultGraph(saved.getId())
                        .then(applyScope(saved.getId(), req.classScope()))
                        .thenReturn(saved))
                .map(CourseResponse::from);
    }

    /** Each course gets one default graph; the knowledge-graph module adds more if needed. */
    private Mono<KnowledgeGraph> provisionDefaultGraph(Long courseId) {
        KnowledgeGraph graph = KnowledgeGraph.builder()
                .id(idGenerator.nextId())
                .courseId(courseId)
                .name("默认图谱")
                .version(1)
                .deleted(0)
                .build();
        return entityTemplate.insert(KnowledgeGraph.class).using(graph);
    }

    // -------------------------------------------------------------- update

    @Transactional
    public Mono<CourseResponse> update(Long id, CourseRequest req, AuthUser editor) {
        BizException dateError = validateDates(req);
        if (dateError != null) {
            return Mono.error(dateError);
        }
        return requireCourseEditable(id, editor)
                .flatMap(course -> {
                    course.setTitle(req.title());
                    course.setCoverUrl(req.coverUrl());
                    course.setIntro(req.intro());
                    if (req.credit() != null) {
                        course.setCredit(req.credit());
                    }
                    if (req.visibility() != null) {
                        course.setVisibility(req.visibility());
                    }
                    course.setStartDate(req.startDate());
                    course.setEndDate(req.endDate());
                    Mono<Void> scope = req.classScope() == null ? Mono.empty()
                            : applyScope(id, req.classScope());
                    return courseRepository.save(course).then(scope).thenReturn(course);
                })
                .map(CourseResponse::from);
    }

    // ------------------------------------------------------ status changes

    public Mono<CourseResponse> publish(Long id, AuthUser editor) {
        return transitionStatus(id, editor, Course.STATUS_PUBLISHED);
    }

    public Mono<CourseResponse> archive(Long id, AuthUser editor) {
        return transitionStatus(id, editor, Course.STATUS_ARCHIVED);
    }

    private Mono<CourseResponse> transitionStatus(Long id, AuthUser editor, int status) {
        return requireCourseEditable(id, editor)
                .flatMap(course -> {
                    course.setStatus(status);
                    return courseRepository.save(course);
                })
                .map(CourseResponse::from);
    }

    // -------------------------------------------------------------- delete

    public Mono<Void> delete(Long id, AuthUser editor) {
        return requireCourseEditable(id, editor)
                .flatMap(course -> {
                    course.setDeleted(1);
                    return courseRepository.save(course);
                })
                .then();
    }

    // ------------------------------------------------------------ helpers

    private Mono<Course> requireCourse(Long id) {
        return courseRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "课程不存在")));
    }

    /** Loads the course and asserts the editor may mutate it (owner/co-teacher/admin). */
    Mono<Course> requireCourseEditable(Long id, AuthUser editor) {
        return requireCourse(id)
                .flatMap(course -> canEdit(course, editor)
                        .flatMap(ok -> ok ? Mono.just(course)
                                : Mono.error(new BizException(BizErrorCode.ACCESS_DENIED, "无权操作该课程"))));
    }

    private Mono<Boolean> canEdit(Course course, AuthUser editor) {
        if (editor == null) {
            return Mono.just(false);
        }
        if (editor.hasRole(ROLE_ADMIN) || course.getTeacherId().equals(editor.id())) {
            return Mono.just(true);
        }
        return relationRepository.isCourseTeacher(course.getId(), editor.id());
    }

    private Mono<Void> applyScope(Long courseId, List<Long> classScope) {
        if (classScope == null) {
            return Mono.empty();
        }
        return relationRepository.replaceScope(courseId, classScope);
    }

    private static BizException validateDates(CourseRequest req) {
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            return new BizException(BizErrorCode.PARAM_INVALID, "结课日期不能早于开课日期");
        }
        return null;
    }
}
