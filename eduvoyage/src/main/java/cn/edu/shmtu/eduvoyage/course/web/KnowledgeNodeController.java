package cn.edu.shmtu.eduvoyage.course.web;

import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeRequest;
import cn.edu.shmtu.eduvoyage.course.dto.KnowledgeNodeDtos.NodeResponse;
import cn.edu.shmtu.eduvoyage.course.service.KnowledgeNodeService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Knowledge points (graph nodes) within a course. Reads are open to authenticated
 * users; authoring requires {@code course:update} plus the service-level course
 * ownership check. Edges between nodes are owned by the knowledge-graph module.
 */
@Tag(name = "知识点管理", description = "课程知识点（图谱节点）的查看与维护")
@RestController
public class KnowledgeNodeController {

    private final KnowledgeNodeService nodeService;

    public KnowledgeNodeController(KnowledgeNodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Operation(summary = "课程知识点列表（可按章节过滤）")
    @GetMapping("/api/courses/{courseId}/nodes")
    public Mono<Result<List<NodeResponse>>> listByCourse(@PathVariable Long courseId,
                                                         @RequestParam(required = false) Long chapterId) {
        return (chapterId == null
                ? nodeService.listByCourse(courseId)
                : nodeService.listByChapter(chapterId))
                .map(Result::success);
    }

    @Operation(summary = "知识点详情")
    @GetMapping("/api/nodes/{id}")
    public Mono<Result<NodeResponse>> get(@PathVariable Long id) {
        return nodeService.get(id).map(Result::success);
    }

    @Operation(summary = "新增知识点")
    @PreAuthorize("hasAuthority('course:update')")
    @PostMapping("/api/courses/{courseId}/nodes")
    public Mono<Result<NodeResponse>> create(@PathVariable Long courseId,
                                             @Valid @RequestBody NodeRequest req,
                                             @AuthenticationPrincipal AuthUser user) {
        return nodeService.create(courseId, req, user).map(Result::success);
    }

    @Operation(summary = "更新知识点")
    @PreAuthorize("hasAuthority('course:update')")
    @PutMapping("/api/nodes/{id}")
    public Mono<Result<NodeResponse>> update(@PathVariable Long id,
                                             @Valid @RequestBody NodeRequest req,
                                             @AuthenticationPrincipal AuthUser user) {
        return nodeService.update(id, req, user).map(Result::success);
    }

    @Operation(summary = "删除知识点")
    @PreAuthorize("hasAuthority('course:update')")
    @DeleteMapping("/api/nodes/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthUser user) {
        return nodeService.delete(id, user).thenReturn(Result.<Void>success());
    }
}
