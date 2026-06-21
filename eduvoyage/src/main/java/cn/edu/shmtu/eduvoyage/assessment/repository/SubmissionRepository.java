package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.Submission;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SubmissionRepository extends ReactiveCrudRepository<Submission, Long> {

    @Query("SELECT * FROM submission WHERE id = :id AND deleted = 0")
    Mono<Submission> findActiveById(Long id);

    @Query("""
            SELECT * FROM submission
            WHERE homework_id = :homeworkId AND student_id = :studentId AND deleted = 0
            ORDER BY attempt_no DESC
            """)
    Flux<Submission> findByHomeworkAndStudent(Long homeworkId, Long studentId);

    @Query("""
            SELECT * FROM submission
            WHERE homework_id = :homeworkId AND student_id = :studentId
              AND status = 0 AND deleted = 0
            ORDER BY attempt_no DESC
            LIMIT 1
            """)
    Mono<Submission> findInProgress(Long homeworkId, Long studentId);

    @Query("""
            SELECT COUNT(*) FROM submission
            WHERE homework_id = :homeworkId AND student_id = :studentId AND deleted = 0
            """)
    Mono<Long> countAttempts(Long homeworkId, Long studentId);

    @Query("SELECT * FROM submission WHERE homework_id = :homeworkId AND deleted = 0 ORDER BY id DESC")
    Flux<Submission> findByHomeworkId(Long homeworkId);
}
