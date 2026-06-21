package cn.edu.shmtu.eduvoyage.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Teacher manual-grading request for the subjective items of a submission. Each
 * entry awards a score (and optional comment) to one answer; the service clamps
 * each score to its question's max, recomputes the submission total and marks the
 * submission graded.
 */
public final class GradingDtos {

    private GradingDtos() {
    }

    @Schema(description = "单题批改")
    public record GradeItem(
            @Schema(description = "题目 id")
            @NotNull(message = "题目 id 不能为空")
            Long questionId,

            @Schema(description = "得分")
            @NotNull(message = "得分不能为空")
            @PositiveOrZero(message = "得分不能为负")
            BigDecimal score,

            @Schema(description = "评语")
            String comment
    ) {
    }

    @Schema(description = "批改请求")
    public record GradeRequest(
            @Schema(description = "各题批改结果")
            @NotEmpty(message = "批改内容不能为空")
            @Valid
            List<GradeItem> grades
    ) {
    }
}
