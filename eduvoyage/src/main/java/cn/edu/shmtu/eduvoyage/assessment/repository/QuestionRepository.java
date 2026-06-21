package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.Question;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Single-entity access for {@code question}. Dynamic question-bank search
 * (optional filters + paging) lives in {@link QuestionQueryRepository}.
 */
public interface QuestionRepository extends ReactiveCrudRepository<Question, Long> {

    @Query("SELECT * FROM question WHERE id = :id AND deleted = 0")
    Mono<Question> findActiveById(Long id);

    @Query("SELECT * FROM question WHERE course_id = :courseId AND deleted = 0 ORDER BY id DESC")
    Flux<Question> findByCourseId(Long courseId);
}
