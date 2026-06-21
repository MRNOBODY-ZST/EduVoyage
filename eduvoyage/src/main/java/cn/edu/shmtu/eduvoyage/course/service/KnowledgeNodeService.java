package cn.edu.shmtu.eduvoyage.course.service;

import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeGraph;
import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeNode;
import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeRequest;
import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeResponse;
import cn.edu.shmtu.eduvoyage.course.repository.CourseChapterRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeGraphRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeNodeRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Knowledge-point (node) management within a course. Nodes are created into the
 * course's default knowledge graph and may be attached to a chapter. Edges
 * between nodes are owned by the knowledge-graph module (next phase).
 */
@Service
public class KnowledgeNodeService {

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeGraphRepository graphRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseService courseService;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public KnowledgeNodeService(KnowledgeNodeRepository nodeRepository,
                                KnowledgeGraphRepository graphRepository,
                                CourseChapterRepository chapterRepository,
                                CourseService courseService,
                                R2dbcEntityTemplate entityTemplate,
                                IdGenerator idGenerator) {
        this.nodeRepository = nodeRepository;
        this.graphRepository = graphRepository;
        this.chapterRepository = chapterRepository;
        this.courseService = courseService;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    public Mono<List<NodeResponse>> listByCourse(Long courseId) {
        return nodeRepository.findByCourseId(courseId).map(NodeResponse::from).collectList();
    }

    public Mono<List<NodeResponse>> listByChapter(Long chapterId) {
        return nodeRepository.findByChapterId(chapterId).map(NodeResponse::from).collectList();
    }

    public Mono<NodeResponse> get(Long id) {
        return requireNode(id).map(NodeResponse::from);
    }

    @Transactional
    public Mono<NodeResponse> create(Long courseId, NodeRequest req, AuthUser editor) {
        return courseService.requireCourseEditable(courseId, editor)
                .then(graphRepository.findByCourseId(courseId)
                        .switchIfEmpty(Mono.error(new BizException(
                                BizErrorCode.SYSTEM_ERROR, "课程缺少默认知识图谱"))))
                .flatMap(graph -> validateChapter(courseId, req.chapterId()).thenReturn(graph))
                .flatMap(graph -> {
                    KnowledgeNode node = KnowledgeNode.builder()
                            .id(idGenerator.nextId())
                            .courseId(courseId)
                            .chapterId(req.chapterId())
                            .graphId(graph.getId())
                            .name(req.name())
                            .description(req.description())
                            .learnGoal(req.learnGoal())
                            .estMinutes(req.estMinutes() == null ? 0 : req.estMinutes())
                            .posX(req.posX())
                            .posY(req.posY())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(KnowledgeNode.class).using(node);
                })
                .map(NodeResponse::from);
    }

    @Transactional
    public Mono<NodeResponse> update(Long id, NodeRequest req, AuthUser editor) {
        return requireNode(id)
                .flatMap(node -> courseService.requireCourseEditable(node.getCourseId(), editor)
                        .then(validateChapter(node.getCourseId(), req.chapterId()))
                        .thenReturn(node))
                .flatMap(node -> {
                    node.setName(req.name());
                    node.setChapterId(req.chapterId());
                    node.setDescription(req.description());
                    node.setLearnGoal(req.learnGoal());
                    if (req.estMinutes() != null) {
                        node.setEstMinutes(req.estMinutes());
                    }
                    if (req.posX() != null) {
                        node.setPosX(req.posX());
                    }
                    if (req.posY() != null) {
                        node.setPosY(req.posY());
                    }
                    return nodeRepository.save(node);
                })
                .map(NodeResponse::from);
    }

    @Transactional
    public Mono<Void> delete(Long id, AuthUser editor) {
        return requireNode(id)
                .flatMap(node -> courseService.requireCourseEditable(node.getCourseId(), editor)
                        .thenReturn(node))
                .flatMap(node -> {
                    node.setDeleted(1);
                    return nodeRepository.save(node);
                })
                .then();
    }

    // ------------------------------------------------------------ helpers

    private Mono<KnowledgeNode> requireNode(Long id) {
        return nodeRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.GRAPH_NODE_NOT_FOUND)));
    }

    /** If a chapter is supplied it must exist and belong to the same course. */
    private Mono<Void> validateChapter(Long courseId, Long chapterId) {
        if (chapterId == null) {
            return Mono.empty();
        }
        return chapterRepository.findActiveById(chapterId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "章节不存在")))
                .flatMap(chapter -> chapter.getCourseId().equals(courseId)
                        ? Mono.empty()
                        : Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "章节不属于该课程")));
    }
}
