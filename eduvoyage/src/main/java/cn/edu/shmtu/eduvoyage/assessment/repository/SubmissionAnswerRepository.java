package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.SubmissionAnswer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SubmissionAnswerRepository extends ReactiveCrudRepository<SubmissionAnswer, Long> {

    @Query("SELECT * FROM submission_answer WHERE submission_id = :submissionId ORDER BY id")
    Flux<SubmissionAnswer> findBySubmissionId(Long submissionId);
}
