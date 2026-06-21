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

import java.time.LocalDateTime;

/**
 * {@code question} — a reusable item in a course's question bank. {@code answer}
 * holds the reference answer for grading: for choice/judge types it stores the
 * canonical option keys (e.g. {@code "A"} or {@code "A,C"}); for fill/short types
 * it stores the expected text. Objective types ({@link #isObjective(int)}) are
 * auto-graded; subjective ones await manual review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("question")
public class Question {

    /** Single choice — exactly one correct option. */
    public static final int TYPE_SINGLE = 1;
    /** Multiple choice — one or more correct options, all required for credit. */
    public static final int TYPE_MULTIPLE = 2;
    /** True/false judgement. */
    public static final int TYPE_JUDGE = 3;
    /** Fill in the blank — graded by exact (trimmed) text match. */
    public static final int TYPE_FILL = 4;
    /** Short answer / essay — manually graded. */
    public static final int TYPE_SHORT = 5;

    /** Whether a question type is machine-gradable. */
    public static boolean isObjective(int type) {
        return type == TYPE_SINGLE || type == TYPE_MULTIPLE || type == TYPE_JUDGE || type == TYPE_FILL;
    }

    @Id
    private Long id;

    @Column("course_id")
    private Long courseId;

    private Integer type;
    private String stem;
    private String answer;
    private String analysis;
    private Integer difficulty;

    @Column("node_id")
    private Long nodeId;

    private String lang;

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
