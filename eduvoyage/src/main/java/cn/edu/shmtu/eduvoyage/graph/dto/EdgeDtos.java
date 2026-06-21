package cn.edu.shmtu.eduvoyage.graph.dto;

import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeEdge;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * Knowledge-edge create request and outbound view. An edge is created inside a
 * course's graph; {@code fromId}/{@code toId} must both be nodes of that graph
 * and, for a {@code PREREQUISITE} edge, must not close a cycle.
 */
public final class EdgeDtos {

    private EdgeDtos() {
    }

    @Schema(description = "知识点关系创建请求")
    public record EdgeRequest(
            @Schema(description = "起点知识点 id（前置）", example = "1001")
            @NotNull(message = "起点知识点不能为空")
            Long fromId,

            @Schema(description = "终点知识点 id（后继）", example = "1002")
            @NotNull(message = "终点知识点不能为空")
            Long toId,

            @Schema(description = "关系类型：PREREQUISITE 前置 / RELATED 关联", example = "PREREQUISITE")
            @NotNull(message = "关系类型不能为空")
            @Pattern(regexp = "PREREQUISITE|RELATED", message = "关系类型非法")
            String type,

            @Schema(description = "权重（默认 1）", example = "1.0")
            Double weight
    ) {
    }

    @Schema(description = "知识点关系信息")
    public record EdgeResponse(
            @Schema(description = "边 id") Long id,
            @Schema(description = "图谱 id") Long graphId,
            @Schema(description = "起点知识点 id") Long fromId,
            @Schema(description = "终点知识点 id") Long toId,
            @Schema(description = "关系类型") String type,
            @Schema(description = "权重") Double weight,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
        public static EdgeResponse from(KnowledgeEdge e) {
            return new EdgeResponse(e.getId(), e.getGraphId(), e.getFromId(), e.getToId(),
                    e.getType(), e.getWeight(), e.getCreatedAt());
        }
    }
}
