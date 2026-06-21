package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.QuestionOption;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Options of a choice/judge question. Options have no soft-delete column, so a
 * question edit replaces its option set with a hard delete + re-insert.
 */
public interface QuestionOptionRepository extends ReactiveCrudRepository<QuestionOption, Long> {

    @Query("SELECT * FROM question_option WHERE question_id = :questionId ORDER BY sort_no, id")
    Flux<QuestionOption> findByQuestionId(Long questionId);

    @Modifying
    @Query("DELETE FROM question_option WHERE question_id = :questionId")
    Mono<Long> deleteByQuestionId(Long questionId);
}
