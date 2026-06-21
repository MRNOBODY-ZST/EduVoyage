package cn.edu.shmtu.eduvoyage.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * {@code submission_answer} — a student's answer to one question within a
 * submission, plus its grade. {@code isCorrect}/{@code score} are filled by the
 * auto-grader for objective items immediately on submit; subjective items get
 * {@code score}/{@code comment} during manual review (no soft-delete column —
 * answers live and die with their submission).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("submission_answer")
public class SubmissionAnswer {

    @Id
    private Long id;

    @Column("submission_id")
    private Long submissionId;

    @Column("question_id")
    private Long questionId;

    private String answer;
    private BigDecimal score;

    @Column("is_correct")
    private Integer isCorrect;

    private String comment;
}
