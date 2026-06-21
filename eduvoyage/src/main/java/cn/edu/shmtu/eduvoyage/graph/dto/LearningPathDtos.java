package cn.edu.shmtu.eduvoyage.graph.dto;

import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Learning-path projections: which nodes a student can study next, the full
 * recommended sequence, and the prerequisite chain leading to a target node.
 * Nodes are returned as lightweight {@link PathNode} entries in study order.
 */
public final class LearningPathDtos {

    private LearningPathDtos() {
    }

    @Schema(description = "学习路径节点")
    public record PathNode(
            @Schema(description = "知识点 id") Long id,
            @Schema(description = "名称") String name,
            @Schema(description = "预计学习分钟数") Integer estMinutes,
            @Schema(description = "是否已掌握") boolean mastered
    ) {
        public static PathNode from(KnowledgeNode n, boolean mastered) {
            return new PathNode(n.getId(), n.getName(), n.getEstMinutes(), mastered);
        }
    }

    @Schema(description = "学习路径推荐")
    public record LearningPath(
            @Schema(description = "图谱 id") Long graphId,
            @Schema(description = "课程 id") Long courseId,
            @Schema(description = "已掌握知识点数") int masteredCount,
            @Schema(description = "知识点总数") int totalCount,
            @Schema(description = "当前可学（前置已满足）") List<PathNode> learnable,
            @Schema(description = "完整推荐顺序（拓扑序，已掌握的已剔除）") List<PathNode> recommended
    ) {
    }

    @Schema(description = "目标知识点的前置链")
    public record PrerequisiteChain(
            @Schema(description = "目标知识点 id") Long targetId,
            @Schema(description = "前置知识点（学习顺序）") List<PathNode> prerequisites
    ) {
    }
}
