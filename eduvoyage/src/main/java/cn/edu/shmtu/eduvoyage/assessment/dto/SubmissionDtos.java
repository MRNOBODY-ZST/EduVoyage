package cn.edu.shmtu.eduvoyage.assessment.dto;

import cn.edu.shmtu.eduvoyage.assessment.domain.Submission;
import cn.edu.shmtu.eduvoyage.assessment.domain.SubmissionAnswer;
import cn.edu.shmtu.eduvoyage.assessment.dto.QuestionDtos.StudentQuestion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Submission flow DTOs: the exam paper a student sees on start, the answers they
 * submit, and the graded result. Objective items are auto-graded on submit; the
 * result view marks subjective items as pending until manual review completes.
 */
public final class SubmissionDtos {

    private SubmissionDtos() {
    }

    @Schema(description = "学生开始作答时的试卷视图")
    public record ExamPaper(
            @Schema(description = "提交记录 id") Long submissionId,
            @Schema(description = "作业 id") Long homeworkId,
            @Schema(description = "标题") String title,
            @Schema(description = "本次尝试序号") Integer attemptNo,
            @Schema(description = "时间限制（分钟）") Integer timeLimit,
            @Schema(description = "截止时间") LocalDateTime deadline,
            @Schema(description = "总分") BigDecimal totalScore,
            @Schema(description = "题目列表（隐藏答案）") List<StudentQuestion> questions
    ) {
    }

    @Schema(description = "单题作答")
    public record AnswerItem(
            @Schema(description = "题目 id")
            @NotNull(message = "题目 id 不能为空")
            Long questionId,

            @Schema(description = "作答内容（选项键或文本）")
            String answer
    ) {
    }

    @Schema(description = "提交作答请求")
    public record SubmitRequest(
            @Schema(description = "作答列表")
            @NotEmpty(message = "作答不能为空")
            @Valid
            List<AnswerItem> answers,

            @Schema(description = "切屏次数（防作弊统计，可空）")
            Integer switchCount
    ) {
    }

    @Schema(description = "作答明细（含评分）")
    public record AnswerResult(
            @Schema(description = "题目 id") Long questionId,
            @Schema(description = "作答内容") String answer,
            @Schema(description = "得分") BigDecimal score,
            @Schema(description = "是否正确（主观题为空）") Integer isCorrect,
            @Schema(description = "评语") String comment
    ) {
        public static AnswerResult from(SubmissionAnswer a) {
            return new AnswerResult(a.getQuestionId(), a.getAnswer(), a.getScore(),
                    a.getIsCorrect(), a.getComment());
        }
    }

    @Schema(description = "提交结果")
    public record SubmissionResult(
            @Schema(description = "提交记录 id") Long id,
            @Schema(description = "作业 id") Long homeworkId,
            @Schema(description = "学生 id") Long studentId,
            @Schema(description = "尝试序号") Integer attemptNo,
            @Schema(description = "状态：0 进行中 1 已提交 2 已批改") Integer status,
            @Schema(description = "总得分") BigDecimal totalScore,
            @Schema(description = "提交时间") LocalDateTime submittedAt,
            @Schema(description = "作答明细") List<AnswerResult> answers
    ) {
        public static SubmissionResult from(Submission s, List<AnswerResult> answers) {
            return new SubmissionResult(s.getId(), s.getHomeworkId(), s.getStudentId(),
                    s.getAttemptNo(), s.getStatus(), s.getTotalScore(), s.getSubmittedAt(), answers);
        }
    }
}
