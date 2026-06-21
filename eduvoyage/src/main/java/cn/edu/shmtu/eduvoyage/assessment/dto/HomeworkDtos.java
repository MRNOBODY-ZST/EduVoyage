package cn.edu.shmtu.eduvoyage.assessment.dto;

import cn.edu.shmtu.eduvoyage.assessment.domain.Homework;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Homework (paper) create/update request and outbound views. The paper's question
 * set is supplied inline as {@link PaperItem}s; the service validates each question
 * exists and totals their scores. {@code totalScore} is derived from the item
 * scores so it always reflects the assembled paper.
 */
public final class HomeworkDtos {

    private HomeworkDtos() {
    }

    @Schema(description = "试卷题目项")
    public record PaperItem(
            @Schema(description = "题目 id")
            @NotNull(message = "题目 id 不能为空")
            Long questionId,

            @Schema(description = "本题分值")
            @NotNull(message = "分值不能为空")
            @Positive(message = "分值必须为正")
            BigDecimal score,

            @Schema(description = "排序")
            Integer sortNo
    ) {
    }

    @Schema(description = "作业/试卷创建/更新请求")
    public record HomeworkRequest(
            @Schema(description = "标题")
            @NotBlank(message = "标题不能为空")
            String title,

            @Schema(description = "时间限制（分钟，可空表示不限时）")
            @PositiveOrZero(message = "时间限制不能为负")
            Integer timeLimit,

            @Schema(description = "截止时间（可空）")
            LocalDateTime deadline,

            @Schema(description = "最大尝试次数", example = "1")
            @Positive(message = "尝试次数必须为正")
            Integer maxAttempts,

            @Schema(description = "是否乱序出题")
            boolean shuffle,

            @Schema(description = "是否启用防切屏")
            boolean antiSwitch,

            @Schema(description = "试卷题目")
            @Valid
            List<PaperItem> items
    ) {
    }

    @Schema(description = "作业/试卷信息（教师视图）")
    public record HomeworkResponse(
            @Schema(description = "作业 id") Long id,
            @Schema(description = "课程 id") Long courseId,
            @Schema(description = "标题") String title,
            @Schema(description = "总分") BigDecimal totalScore,
            @Schema(description = "时间限制（分钟）") Integer timeLimit,
            @Schema(description = "截止时间") LocalDateTime deadline,
            @Schema(description = "最大尝试次数") Integer maxAttempts,
            @Schema(description = "是否乱序") boolean shuffle,
            @Schema(description = "是否防切屏") boolean antiSwitch,
            @Schema(description = "状态：0 草稿 1 已发布 2 已关闭") Integer status,
            @Schema(description = "题目数量") int questionCount,
            @Schema(description = "创建时间") LocalDateTime createdAt,
            @Schema(description = "试卷题目项") List<PaperItem> items
    ) {
        public static HomeworkResponse from(Homework h, List<PaperItem> items) {
            List<PaperItem> paperItems = items == null ? List.of() : items;
            return new HomeworkResponse(h.getId(), h.getCourseId(), h.getTitle(), h.getTotalScore(),
                    h.getTimeLimit(), h.getDeadline(), h.getMaxAttempts(),
                    h.getShuffle() != null && h.getShuffle() == 1,
                    h.getAntiSwitch() != null && h.getAntiSwitch() == 1,
                    h.getStatus(), paperItems.size(), h.getCreatedAt(), paperItems);
        }
    }
}
