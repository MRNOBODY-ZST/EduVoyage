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
 * {@code homework} — an assignment/exam paper for a course, assembled from
 * question-bank items via {@code homework_question}. Only a {@link #STATUS_PUBLISHED}
 * homework accepts submissions, and only before its {@code deadline}. {@code timeLimit}
 * (minutes, optional) bounds a single attempt; {@code maxAttempts} caps retries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("homework")
public class Homework {

    /** Editable draft, not visible to students. */
    public static final int STATUS_DRAFT = 0;
    /** Published and accepting submissions (subject to deadline/attempts). */
    public static final int STATUS_PUBLISHED = 1;
    /** Closed — no further submissions accepted. */
    public static final int STATUS_CLOSED = 2;

    @Id
    private Long id;

    @Column("course_id")
    private Long courseId;

    private String title;

    @Column("total_score")
    private BigDecimal totalScore;

    @Column("time_limit")
    private Integer timeLimit;

    private LocalDateTime deadline;

    @Column("max_attempts")
    private Integer maxAttempts;

    private Integer shuffle;

    @Column("anti_switch")
    private Integer antiSwitch;

    private Integer status;

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
