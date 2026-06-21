package cn.edu.shmtu.eduvoyage.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * {@code homework_question} — the per-paper inclusion of a question, carrying the
 * points it's worth and its position. Composite primary key
 * {@code (homework_id, question_id)}; all access goes through a hand-written
 * {@code DatabaseClient} repository since R2DBC has no composite-id mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("homework_question")
public class HomeworkQuestion {

    @Column("homework_id")
    private Long homeworkId;

    @Column("question_id")
    private Long questionId;

    private BigDecimal score;

    @Column("sort_no")
    private Integer sortNo;
}
