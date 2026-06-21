package cn.edu.shmtu.eduvoyage.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code submission} — one student attempt at a homework. Created in
 * {@link #STATUS_IN_PROGRESS} when the student starts, then transitions to
 * {@link #STATUS_SUBMITTED} (objective auto-graded) and finally
 * {@link #STATUS_GRADED} once any subjective items are reviewed. {@code attemptNo}
 * counts retries against {@code homework.max_attempts}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("submission")
public class Submission {

    /** Started, not yet submitted. */
    public static final int STATUS_IN_PROGRESS = 0;
    /** Submitted; objective parts auto-graded, subjective pending. */
    public static final int STATUS_SUBMITTED = 1;
    /** Fully graded (manual review done). */
    public static final int STATUS_GRADED = 2;

    @Id
    private Long id;

    @Column("homework_id")
    private Long homeworkId;

    @Column("student_id")
    private Long studentId;

    @Column("attempt_no")
    private Integer attemptNo;

    private Integer status;

    @Column("total_score")
    private BigDecimal totalScore;

    @Column("submitted_at")
    private LocalDateTime submittedAt;

    @Column("started_at")
    private LocalDateTime startedAt;

    @Column("switch_count")
    private Integer switchCount;

    @CreatedBy
    @Column("created_by")
    private Long createdBy;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    private Integer deleted;
}
