package cn.edu.shmtu.eduvoyage.course.dto;

import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Knowledge-point (node) create/update request and outbound view. A node always
 * belongs to its course's knowledge graph; the optional {@code chapterId} ties it
 * to a chapter for the syllabus view.
 */
public final class KnowledgeNodeDtos {

    private KnowledgeNodeDtos() {
    }

    @Schema(description = "知识点创建/更新请求")
    public record NodeRequest(
            @Schema(description = "知识点名称", example = "二叉树的遍历")
            @NotBlank(message = "知识点名称不能为空")
            @Size(max = 200, message = "名称过长")
            String name,

            @Schema(description = "所属章节 id（可空）")
            Long chapterId,

            @Schema(description = "描述")
            String description,

            @Schema(description = "学习目标")
            @Size(max = 512, message = "学习目标过长")
            String learnGoal,

            @Schema(description = "预计学习分钟数")
            @PositiveOrZero(message = "预计时长不能为负")
            Integer estMinutes,

            @Schema(description = "图谱布局横坐标")
            Double posX,

            @Schema(description = "图谱布局纵坐标")
            Double posY
    ) {
    }

    @Schema(description = "知识点信息")
    public record NodeResponse(
            @Schema(description = "知识点 id") Long id,
            @Schema(description = "课程 id") Long courseId,
            @Schema(description = "章节 id") Long chapterId,
            @Schema(description = "图谱 id") Long graphId,
            @Schema(description = "名称") String name,
            @Schema(description = "描述") String description,
            @Schema(description = "学习目标") String learnGoal,
            @Schema(description = "预计分钟数") Integer estMinutes,
            @Schema(description = "横坐标") Double posX,
            @Schema(description = "纵坐标") Double posY,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
        public static NodeResponse from(KnowledgeNode n) {
            return new NodeResponse(n.getId(), n.getCourseId(), n.getChapterId(), n.getGraphId(),
                    n.getName(), n.getDescription(), n.getLearnGoal(), n.getEstMinutes(),
                    n.getPosX(), n.getPosY(), n.getCreatedAt());
        }
    }
}
