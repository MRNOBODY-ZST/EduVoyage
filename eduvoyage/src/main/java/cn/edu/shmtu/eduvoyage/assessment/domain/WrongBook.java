package cn.edu.shmtu.eduvoyage.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * {@code wrong_book} — a student's personal record of a question they've missed.
 * Upserted on each wrong answer (the unique key {@code (student_id, question_id)}
 * means {@code wrongCount} accumulates in place), and flagged {@code mastered}
 * once the student gets it right again. {@code nodeId} links back to the knowledge
 * point so the wrong book can be browsed by topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("wrong_book")
public class WrongBook {

    @Id
    private Long id;

    @Column("student_id")
    private Long studentId;

    @Column("question_id")
    private Long questionId;

    @Column("node_id")
    private Long nodeId;

    @Column("wrong_count")
    private Integer wrongCount;

    @Column("last_wrong_at")
    private LocalDateTime lastWrongAt;

    private Integer mastered;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
