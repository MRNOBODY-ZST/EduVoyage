package cn.edu.shmtu.eduvoyage.graph.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code knowledge_mastery} — one student's grasp of one knowledge node. The unique
 * key {@code (student_id, node_id)} means each pairing is upserted in place. The
 * graph module rolls these up to drive learning-path recommendation: a node counts
 * as "mastered" once its {@code masteryLevel} reaches {@link #LEVEL_MASTERED}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_mastery")
public class KnowledgeMastery {

    /** Not started. */
    public static final int LEVEL_UNKNOWN = 0;
    /** Currently learning. */
    public static final int LEVEL_LEARNING = 1;
    /** Sufficiently mastered to unlock dependents. */
    public static final int LEVEL_MASTERED = 2;

    @Id
    private Long id;

    @Column("student_id")
    private Long studentId;

    @Column("node_id")
    private Long nodeId;

    @Column("mastery_level")
    private Integer masteryLevel;

    private BigDecimal score;

    @Column("learn_progress")
    private BigDecimal learnProgress;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    private Integer deleted;
}
