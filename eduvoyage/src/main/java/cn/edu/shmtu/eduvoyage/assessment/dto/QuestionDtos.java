package cn.edu.shmtu.eduvoyage.assessment.dto;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import cn.edu.shmtu.eduvoyage.assessment.domain.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Question-bank create/update request and outbound views. The {@code answer}
 * field carries the canonical answer used for objective auto-grading; for
 * choice/judge types it is the option-key string (e.g. {@code "A"} / {@code "A,C"})
 * and the {@code options} list supplies the selectable choices. A separate
 * {@link StudentQuestion} view omits {@code answer}/{@code analysis} and the
 * {@code isCorrect} flags so it is safe to expose mid-exam.
 */
public final class QuestionDtos {

    private QuestionDtos() {
    }

    @Schema(description = "题目选项请求")
    public record OptionRequest(
            @Schema(description = "选项标识，如 A/B/C", example = "A")
            @NotBlank(message = "选项标识不能为空")
            @Size(max = 8, message = "选项标识过长")
            String optionKey,

            @Schema(description = "选项内容")
            @NotBlank(message = "选项内容不能为空")
            String content,

            @Schema(description = "是否正确选项")
            boolean correct,

            @Schema(description = "排序")
            Integer sortNo
    ) {
    }

    @Schema(description = "题目创建/更新请求")
    public record QuestionRequest(
            @Schema(description = "所属课程 id（可空，公共题库为 null）")
            Long courseId,

            @Schema(description = "题型：1 单选 2 多选 3 判断 4 填空 5 简答", example = "1")
            @NotNull(message = "题型不能为空")
            @Min(value = 1, message = "题型非法")
            @Max(value = 5, message = "题型非法")
            Integer type,

            @Schema(description = "题干")
            @NotBlank(message = "题干不能为空")
            String stem,

            @Schema(description = "参考答案（客观题为选项键/文本，主观题可空）")
            String answer,

            @Schema(description = "解析")
            String analysis,

            @Schema(description = "难度 1-5", example = "1")
            @Min(value = 1, message = "难度非法")
            @Max(value = 5, message = "难度非法")
            Integer difficulty,

            @Schema(description = "关联知识点 id（可空）")
            Long nodeId,

            @Schema(description = "语言标签（如代码题），可空")
            String lang,

            @Schema(description = "选项（选择/判断题）")
            @Valid
            List<OptionRequest> options
    ) {
    }

    @Schema(description = "题目选项（教师视图）")
    public record OptionView(
            @Schema(description = "选项 id") Long id,
            @Schema(description = "选项标识") String optionKey,
            @Schema(description = "内容") String content,
            @Schema(description = "是否正确") boolean correct,
            @Schema(description = "排序") Integer sortNo
    ) {
        public static OptionView from(QuestionOption o) {
            return new OptionView(o.getId(), o.getOptionKey(), o.getContent(),
                    o.getIsCorrect() != null && o.getIsCorrect() == 1, o.getSortNo());
        }
    }

    @Schema(description = "题目（教师视图，含答案）")
    public record QuestionResponse(
            @Schema(description = "题目 id") Long id,
            @Schema(description = "课程 id") Long courseId,
            @Schema(description = "题型") Integer type,
            @Schema(description = "题干") String stem,
            @Schema(description = "参考答案") String answer,
            @Schema(description = "解析") String analysis,
            @Schema(description = "难度") Integer difficulty,
            @Schema(description = "知识点 id") Long nodeId,
            @Schema(description = "语言标签") String lang,
            @Schema(description = "选项") List<OptionView> options,
            @Schema(description = "创建时间") LocalDateTime createdAt
    ) {
        public static QuestionResponse from(Question q, List<OptionView> options) {
            return new QuestionResponse(q.getId(), q.getCourseId(), q.getType(), q.getStem(),
                    q.getAnswer(), q.getAnalysis(), q.getDifficulty(), q.getNodeId(), q.getLang(),
                    options, q.getCreatedAt());
        }
    }

    @Schema(description = "题目选项（学生视图，不含正误）")
    public record StudentOption(
            @Schema(description = "选项标识") String optionKey,
            @Schema(description = "内容") String content
    ) {
        public static StudentOption from(QuestionOption o) {
            return new StudentOption(o.getOptionKey(), o.getContent());
        }
    }

    @Schema(description = "题目（学生答题视图，隐藏答案）")
    public record StudentQuestion(
            @Schema(description = "题目 id") Long id,
            @Schema(description = "题型") Integer type,
            @Schema(description = "题干") String stem,
            @Schema(description = "难度") Integer difficulty,
            @Schema(description = "语言标签") String lang,
            @Schema(description = "本题分值") java.math.BigDecimal score,
            @Schema(description = "选项") List<StudentOption> options
    ) {
        public static StudentQuestion from(Question q, java.math.BigDecimal score, List<StudentOption> options) {
            return new StudentQuestion(q.getId(), q.getType(), q.getStem(), q.getDifficulty(),
                    q.getLang(), score, options);
        }
    }
}
