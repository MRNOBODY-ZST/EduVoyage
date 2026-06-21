package cn.edu.shmtu.eduvoyage.graph.web;

import cn.edu.shmtu.eduvoyage.graph.dto.MasteryDtos.MasteryRequest;
import cn.edu.shmtu.eduvoyage.graph.dto.MasteryDtos.MasteryResponse;
import cn.edu.shmtu.eduvoyage.graph.service.MasteryService;
import cn.edu.shmtu.eduvoyage.shared.api.Result;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Per-student knowledge-point mastery. A student reports and reads their own
 * mastery; the calling principal is always the subject, so there is no way to
 * read or write another student's record through these routes.
 */
@Tag(name = "知识点掌握度", description = "学生对知识点的掌握度上报与查询")
@RestController
public class MasteryController {

    private final MasteryService masteryService;

    public MasteryController(MasteryService masteryService) {
        this.masteryService = masteryService;
    }

    @Operation(summary = "上报知识点掌握度")
    @PreAuthorize("hasAuthority('graph:read')")
    @PutMapping("/api/nodes/{nodeId}/mastery")
    public Mono<Result<MasteryResponse>> report(@PathVariable Long nodeId,
                                                @Valid @RequestBody MasteryRequest req,
                                                @AuthenticationPrincipal AuthUser user) {
        return masteryService.report(nodeId, req, user.id()).map(Result::success);
    }

    @Operation(summary = "查询我对某知识点的掌握度")
    @PreAuthorize("hasAuthority('graph:read')")
    @GetMapping("/api/nodes/{nodeId}/mastery/me")
    public Mono<Result<MasteryResponse>> mine(@PathVariable Long nodeId,
                                              @AuthenticationPrincipal AuthUser user) {
        return masteryService.get(nodeId, user.id()).map(Result::success);
    }

    @Operation(summary = "查询我在某图谱下的全部掌握度")
    @PreAuthorize("hasAuthority('graph:read')")
    @GetMapping("/api/graph/{graphId}/mastery/me")
    public Mono<Result<List<MasteryResponse>>> mineByGraph(@PathVariable Long graphId,
                                                           @AuthenticationPrincipal AuthUser user) {
        return masteryService.listByGraph(graphId, user.id()).map(Result::success);
    }
}
