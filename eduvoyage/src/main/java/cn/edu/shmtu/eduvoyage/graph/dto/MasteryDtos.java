package cn.edu.shmtu.eduvoyage.graph.dto;

import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeMastery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mastery upsert request and outbound view. A student (or a teacher acting on
 * their behalf) reports progress on a single knowledge node; the service upserts
 * the {@code (student, node)} row.
 */
public final class MasteryDtos {

    private MasteryDtos() {
    }

    @Schema(description = "知识点掌握度上报")
    public record MasteryRequest(
            @Schema(description = "掌握等级：0 未开始 / 1 学习中 / 2 已掌握", example = "1")
            @NotNull(message = "掌握等级不能为空")
            @Min(value = 0, message = "掌握等级非法")
            @Max(value = 2, message = "掌握等级非法")
            Integer masteryLevel,

            @Schema(description = "得分（0-100，可空）", example = "85.0")
            @DecimalMin(value = "0.0", message = "得分不能为负")
            @DecimalMax(value = "100.0", message = "得分不能超过 100")
            BigDecimal score,

            @Schema(description = "学习进度百分比（0-100）", example = "60.0")
            @DecimalMin(value = "0.0", message = "进度不能为负")
            @DecimalMax(value = "100.0", message = "进度不能超过 100")
            BigDecimal learnProgress
    ) {
    }

    @Schema(description = "知识点掌握度信息")
    public record MasteryResponse(
            @Schema(description = "记录 id") Long id,
            @Schema(description = "学生 id") Long studentId,
            @Schema(description = "知识点 id") Long nodeId,
            @Schema(description = "掌握等级") Integer masteryLevel,
            @Schema(description = "得分") BigDecimal score,
            @Schema(description = "学习进度") BigDecimal learnProgress,
            @Schema(description = "更新时间") LocalDateTime updatedAt
    ) {
        public static MasteryResponse from(KnowledgeMastery m) {
            return new MasteryResponse(m.getId(), m.getStudentId(), m.getNodeId(),
                    m.getMasteryLevel(), m.getScore(), m.getLearnProgress(), m.getUpdatedAt());
        }
    }
}
