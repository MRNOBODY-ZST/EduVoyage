package cn.edu.shmtu.eduvoyage.graph.dto;

import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeNode;
import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeEdge;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Read-side projections of a whole knowledge graph for the canvas view and for
 * graph-analysis endpoints (topological order, prerequisite chains).
 */
public final class GraphViewDtos {

    private GraphViewDtos() {
    }

    @Schema(description = "图谱节点（画布视图）")
    public record GraphNode(
            @Schema(description = "知识点 id") Long id,
            @Schema(description = "名称") String name,
            @Schema(description = "所属章节 id") Long chapterId,
            @Schema(description = "预计学习分钟数") Integer estMinutes,
            @Schema(description = "横坐标") Double posX,
            @Schema(description = "纵坐标") Double posY
    ) {
        public static GraphNode from(KnowledgeNode n) {
            return new GraphNode(n.getId(), n.getName(), n.getChapterId(),
                    n.getEstMinutes(), n.getPosX(), n.getPosY());
        }
    }

    @Schema(description = "图谱边（画布视图）")
    public record GraphLink(
            @Schema(description = "边 id") Long id,
            @Schema(description = "起点知识点 id") Long fromId,
            @Schema(description = "终点知识点 id") Long toId,
            @Schema(description = "关系类型") String type,
            @Schema(description = "权重") Double weight
    ) {
        public static GraphLink from(KnowledgeEdge e) {
            return new GraphLink(e.getId(), e.getFromId(), e.getToId(), e.getType(), e.getWeight());
        }
    }

    @Schema(description = "完整知识图谱视图")
    public record GraphView(
            @Schema(description = "图谱 id") Long graphId,
            @Schema(description = "课程 id") Long courseId,
            @Schema(description = "图谱名称") String name,
            @Schema(description = "节点列表") List<GraphNode> nodes,
            @Schema(description = "边列表") List<GraphLink> links
    ) {
    }
}
