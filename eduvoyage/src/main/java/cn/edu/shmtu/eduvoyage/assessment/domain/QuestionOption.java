package cn.edu.shmtu.eduvoyage.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * {@code question_option} — one selectable option of a choice/judge question.
 * Physically deleted with its parent question (no soft-delete column); the
 * {@code isCorrect} flag is the source of truth for the option set, while the
 * parent {@code question.answer} caches the canonical key string for grading.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("question_option")
public class QuestionOption {

    @Id
    private Long id;

    @Column("question_id")
    private Long questionId;

    @Column("option_key")
    private String optionKey;

    private String content;

    @Column("is_correct")
    private Integer isCorrect;

    @Column("sort_no")
    private Integer sortNo;
}
