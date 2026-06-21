package cn.edu.shmtu.eduvoyage.graph.web;

import cn.edu.shmtu.eduvoyage.graph.dto.EdgeDtos.EdgeRequest;
import cn.edu.shmtu.eduvoyage.graph.dto.EdgeDtos.EdgeResponse;
import cn.edu.shmtu.eduvoyage.graph.dto.GraphViewDtos.GraphView;
import cn.edu.shmtu.eduvoyage.graph.dto.LearningPathDtos.LearningPath;
import cn.edu.shmtu.eduvoyage.graph.dto.LearningPathDtos.PathNode;
import cn.edu.shmtu.eduvoyage.graph.dto.LearningPathDtos.PrerequisiteChain;
import cn.edu.shmtu.eduvoyage.graph.service.KnowledgeGraphService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Knowledge-graph authoring and analysis. Reads (the canvas view, topological
 * order, prerequisite chains, learning path) require {@code graph:read}; edge
 * mutation requires {@code graph:edit} plus the service-level course-ownership
 * check. Learning-path recommendation is computed for the calling student.
 */
@Tag(name = "知识图谱", description = "知识点关系、图谱分析与学习路径")
@RestController
public class KnowledgeGraphController {

    private final KnowledgeGraphService graphService;

    public KnowledgeGraphController(KnowledgeGraphService graphService) {
        this.graphService = graphService;
    }

    @Operation(summary = "课程知识图谱（节点+边）")
    @PreAuthorize("hasAuthority('graph:read')")
    @GetMapping("/api/courses/{courseId}/graph")
    public Mono<Result<GraphView>> view(@PathVariable Long courseId) {
        return graphService.view(courseId).map(Result::success);
    }

    @Operation(summary = "图谱关系列表")
    @PreAuthorize("hasAuthority('graph:read')")
    @GetMapping("/api/courses/{courseId}/graph/edges")
    public Mono<Result<List<EdgeResponse>>> listEdges(@PathVariable Long courseId) {
        return graphService.listEdges(courseId).map(Result::success);
    }

    @Operation(summary = "新增知识点关系（前置/关联）")
    @PreAuthorize("hasAuthority('graph:edit')")
    @PostMapping("/api/courses/{courseId}/graph/edges")
    public Mono<Result<EdgeResponse>> addEdge(@PathVariable Long courseId,
                                              @Valid @RequestBody EdgeRequest req,
                                              @AuthenticationPrincipal AuthUser user) {
        return graphService.addEdge(courseId, req, user).map(Result::success);
    }

    @Operation(summary = "删除知识点关系")
    @PreAuthorize("hasAuthority('graph:edit')")
    @DeleteMapping("/api/graph/edges/{edgeId}")
    public Mono<Result<Void>> deleteEdge(@PathVariable Long edgeId,
                                         @AuthenticationPrincipal AuthUser user) {
        return graphService.deleteEdge(edgeId, user).thenReturn(Result.<Void>success());
    }

    @Operation(summary = "图谱拓扑学习顺序")
    @PreAuthorize("hasAuthority('graph:read')")
    @GetMapping("/api/courses/{courseId}/graph/topo-order")
    public Mono<Result<List<PathNode>>> topologicalOrder(@PathVariable Long courseId) {
        return graphService.topologicalOrder(courseId).map(Result::success);
    }

    @Operation(summary = "某知识点的前置链")
    @PreAuthorize("hasAuthority('graph:read')")
    @GetMapping("/api/courses/{courseId}/graph/prerequisites/{nodeId}")
    public Mono<Result<PrerequisiteChain>> prerequisiteChain(@PathVariable Long courseId,
                                                             @PathVariable Long nodeId) {
        return graphService.prerequisiteChain(courseId, nodeId).map(Result::success);
    }

    @Operation(summary = "我的学习路径推荐")
    @PreAuthorize("hasAuthority('graph:read')")
    @GetMapping("/api/courses/{courseId}/graph/learning-path")
    public Mono<Result<LearningPath>> myLearningPath(@PathVariable Long courseId,
                                                     @AuthenticationPrincipal AuthUser user) {
        return graphService.learningPath(courseId, user.id()).map(Result::success);
    }
}
