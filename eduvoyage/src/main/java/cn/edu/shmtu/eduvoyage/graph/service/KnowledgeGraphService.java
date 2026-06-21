package cn.edu.shmtu.eduvoyage.graph.service;

import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeGraph;
import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeNode;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeGraphRepository;
import cn.edu.shmtu.eduvoyage.course.repository.KnowledgeNodeRepository;
import cn.edu.shmtu.eduvoyage.course.service.CourseService;
import cn.edu.shmtu.eduvoyage.graph.algo.KnowledgeGraphModel;
import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeEdge;
import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeMastery;
import cn.edu.shmtu.eduvoyage.graph.dto.EdgeDtos.EdgeRequest;
import cn.edu.shmtu.eduvoyage.graph.dto.EdgeDtos.EdgeResponse;
import cn.edu.shmtu.eduvoyage.graph.dto.GraphViewDtos.GraphLink;
import cn.edu.shmtu.eduvoyage.graph.dto.GraphViewDtos.GraphNode;
import cn.edu.shmtu.eduvoyage.graph.dto.GraphViewDtos.GraphView;
import cn.edu.shmtu.eduvoyage.graph.dto.LearningPathDtos.LearningPath;
import cn.edu.shmtu.eduvoyage.graph.dto.LearningPathDtos.PathNode;
import cn.edu.shmtu.eduvoyage.graph.dto.LearningPathDtos.PrerequisiteChain;
import cn.edu.shmtu.eduvoyage.graph.repository.KnowledgeEdgeRepository;
import cn.edu.shmtu.eduvoyage.graph.repository.KnowledgeMasteryRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Knowledge-graph authoring and analysis. Owns the edges between knowledge nodes
 * and the algorithms over them: cycle-safe prerequisite linking, the canvas view,
 * topological study order, prerequisite chains and per-student learning-path
 * recommendation.
 *
 * <p>A course has exactly one default graph (provisioned by the course module),
 * which is the unit of analysis here. Authoring authorization is delegated to
 * {@link CourseService#requireCourseEditable} so the owner/co-teacher/ADMIN rule
 * stays in one place; reads only require the caller to be authenticated.</p>
 */
@Service
public class KnowledgeGraphService {

    private final KnowledgeGraphRepository graphRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final KnowledgeMasteryRepository masteryRepository;
    private final CourseService courseService;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public KnowledgeGraphService(KnowledgeGraphRepository graphRepository,
                                 KnowledgeNodeRepository nodeRepository,
                                 KnowledgeEdgeRepository edgeRepository,
                                 KnowledgeMasteryRepository masteryRepository,
                                 CourseService courseService,
                                 R2dbcEntityTemplate entityTemplate,
                                 IdGenerator idGenerator) {
        this.graphRepository = graphRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.masteryRepository = masteryRepository;
        this.courseService = courseService;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    // ----------------------------------------------------------- graph view

    /** The full graph (nodes + edges) for a course's default graph. */
    public Mono<GraphView> view(Long courseId) {
        return requireGraph(courseId).flatMap(graph -> {
            Mono<List<GraphNode>> nodes = nodeRepository.findByGraphId(graph.getId())
                    .map(GraphNode::from).collectList();
            Mono<List<GraphLink>> links = edgeRepository.findByGraphId(graph.getId())
                    .map(GraphLink::from).collectList();
            return Mono.zip(nodes, links)
                    .map(t -> new GraphView(graph.getId(), courseId, graph.getName(), t.getT1(), t.getT2()));
        });
    }

    public Mono<List<EdgeResponse>> listEdges(Long courseId) {
        return requireGraph(courseId)
                .flatMapMany(graph -> edgeRepository.findByGraphId(graph.getId()))
                .map(EdgeResponse::from)
                .collectList();
    }

    // -------------------------------------------------------- edge mutation

    /**
     * Adds an edge inside the course's graph after validating both endpoints
     * belong to that graph, the edge does not already exist, and — for a
     * prerequisite edge — it would not introduce a cycle.
     */
    @Transactional
    public Mono<EdgeResponse> addEdge(Long courseId, EdgeRequest req, AuthUser editor) {
        if (req.fromId().equals(req.toId())) {
            return Mono.error(new BizException(BizErrorCode.GRAPH_CYCLE, "起点和终点不能相同"));
        }
        return courseService.requireCourseEditable(courseId, editor)
                .then(requireGraph(courseId))
                .flatMap(graph -> requireNodeInGraph(req.fromId(), graph.getId())
                        .then(requireNodeInGraph(req.toId(), graph.getId()))
                        .then(Mono.defer(() -> edgeRepository.findActiveEdge(req.fromId(), req.toId(), req.type()))
                                .flatMap(existing -> Mono.<EdgeResponse>error(
                                        new BizException(BizErrorCode.GRAPH_EDGE_EXISTS)))
                                .switchIfEmpty(Mono.defer(() -> insertEdgeChecked(graph, req)))));
    }

    private Mono<EdgeResponse> insertEdgeChecked(KnowledgeGraph graph, EdgeRequest req) {
        Mono<Void> cycleGuard = KnowledgeEdge.TYPE_PREREQUISITE.equals(req.type())
                ? prerequisiteEdges(graph.getId())
                        .flatMap(model -> model.wouldCreateCycle(req.fromId(), req.toId())
                                ? Mono.error(new BizException(BizErrorCode.GRAPH_CYCLE))
                                : Mono.empty())
                : Mono.empty();
        return cycleGuard.then(Mono.defer(() -> {
            KnowledgeEdge edge = KnowledgeEdge.builder()
                    .id(idGenerator.nextId())
                    .graphId(graph.getId())
                    .fromId(req.fromId())
                    .toId(req.toId())
                    .type(req.type())
                    .weight(req.weight() == null ? 1.0 : req.weight())
                    .deleted(0)
                    .build();
            return entityTemplate.insert(KnowledgeEdge.class).using(edge);
        })).map(EdgeResponse::from);
    }

    @Transactional
    public Mono<Void> deleteEdge(Long edgeId, AuthUser editor) {
        return edgeRepository.findActiveById(edgeId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.GRAPH_EDGE_EXISTS, "关系不存在")))
                .flatMap(edge -> resolveCourseId(edge.getGraphId())
                        .flatMap(courseId -> courseService.requireCourseEditable(courseId, editor))
                        .then(Mono.defer(() -> {
                            edge.setDeleted(1);
                            return edgeRepository.save(edge);
                        })))
                .then();
    }

    // ------------------------------------------------------- graph analysis

    /** Topological study order of the graph's nodes; errors if the graph is cyclic. */
    public Mono<List<PathNode>> topologicalOrder(Long courseId) {
        return requireGraph(courseId).flatMap(graph -> Mono.zip(
                        nodeMap(graph.getId()),
                        prerequisiteEdges(graph.getId()))
                .map(t -> {
                    Map<Long, KnowledgeNode> byId = t.getT1();
                    List<Long> order = t.getT2().topologicalOrder();
                    if (order == null) {
                        throw new BizException(BizErrorCode.GRAPH_CYCLE, "图谱存在环，无法排序");
                    }
                    return toPathNodes(order, byId, Set.of());
                }));
    }

    /** The ordered transitive prerequisites of a target node. */
    public Mono<PrerequisiteChain> prerequisiteChain(Long courseId, Long targetNodeId) {
        return requireGraph(courseId).flatMap(graph -> requireNodeInGraph(targetNodeId, graph.getId())
                .then(Mono.zip(nodeMap(graph.getId()), prerequisiteEdges(graph.getId())))
                .map(t -> {
                    Map<Long, KnowledgeNode> byId = t.getT1();
                    List<Long> chain = t.getT2().prerequisiteChain(targetNodeId);
                    return new PrerequisiteChain(targetNodeId, toPathNodes(chain, byId, Set.of()));
                }));
    }

    /**
     * A learning-path recommendation for a student: currently-learnable nodes
     * (all prerequisites mastered) plus the full recommended sequence with
     * already-mastered nodes removed.
     */
    public Mono<LearningPath> learningPath(Long courseId, Long studentId) {
        return requireGraph(courseId).flatMap(graph -> Mono.zip(
                        nodeMap(graph.getId()),
                        prerequisiteEdges(graph.getId()),
                        masteredNodeIds(studentId, graph.getId()))
                .map(t -> {
                    Map<Long, KnowledgeNode> byId = t.getT1();
                    KnowledgeGraphModel model = t.getT2();
                    Set<Long> mastered = t.getT3();
                    List<Long> learnable = model.learnableNodes(mastered);
                    List<Long> recommended = model.recommendedPath(mastered);
                    if (recommended == null) {
                        throw new BizException(BizErrorCode.GRAPH_CYCLE, "图谱存在环，无法推荐路径");
                    }
                    return new LearningPath(
                            graph.getId(), courseId,
                            mastered.size(), byId.size(),
                            toPathNodes(learnable, byId, mastered),
                            toPathNodes(recommended, byId, mastered));
                }));
    }

    // ------------------------------------------------------------ helpers

    Mono<KnowledgeGraph> requireGraph(Long courseId) {
        return graphRepository.findByCourseId(courseId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "课程图谱不存在")));
    }

    private Mono<Long> resolveCourseId(Long graphId) {
        return graphRepository.findActiveById(graphId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "图谱不存在")))
                .map(KnowledgeGraph::getCourseId);
    }

    private Mono<KnowledgeNode> requireNodeInGraph(Long nodeId, Long graphId) {
        return nodeRepository.findActiveById(nodeId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.GRAPH_NODE_NOT_FOUND)))
                .flatMap(node -> node.getGraphId() != null && node.getGraphId().equals(graphId)
                        ? Mono.just(node)
                        : Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "知识点不属于该图谱")));
    }

    private Mono<Map<Long, KnowledgeNode>> nodeMap(Long graphId) {
        return nodeRepository.findByGraphId(graphId)
                .collect(LinkedHashMap::new, (map, node) -> map.put(node.getId(), node));
    }

    /** Builds the algorithm model from all nodes plus the graph's prerequisite edges. */
    private Mono<KnowledgeGraphModel> prerequisiteEdges(Long graphId) {
        Mono<List<Long>> nodeIds = nodeRepository.findByGraphId(graphId)
                .map(KnowledgeNode::getId).collectList();
        Mono<List<long[]>> edges = edgeRepository
                .findByGraphIdAndType(graphId, KnowledgeEdge.TYPE_PREREQUISITE)
                .map(e -> new long[]{e.getFromId(), e.getToId()})
                .collectList();
        return Mono.zip(nodeIds, edges)
                .map(t -> KnowledgeGraphModel.of(t.getT1(), t.getT2()));
    }

    private Mono<Set<Long>> masteredNodeIds(Long studentId, Long graphId) {
        return masteryRepository.findByStudentAndGraph(studentId, graphId)
                .filter(m -> m.getMasteryLevel() != null
                        && m.getMasteryLevel() >= KnowledgeMastery.LEVEL_MASTERED)
                .map(m -> m.getNodeId())
                .collect(Collectors.toSet());
    }

    private static List<PathNode> toPathNodes(List<Long> ids, Map<Long, KnowledgeNode> byId, Set<Long> mastered) {
        List<PathNode> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            KnowledgeNode node = byId.get(id);
            if (node != null) {
                out.add(PathNode.from(node, mastered.contains(id)));
            }
        }
        return out;
    }
}
