package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.WrongBook;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WrongBookRepository extends ReactiveCrudRepository<WrongBook, Long> {

    @Query("SELECT * FROM wrong_book WHERE student_id = :studentId AND question_id = :questionId")
    Mono<WrongBook> findByStudentAndQuestion(Long studentId, Long questionId);

    @Query("SELECT * FROM wrong_book WHERE student_id = :studentId ORDER BY last_wrong_at DESC")
    Flux<WrongBook> findByStudent(Long studentId);

    @Query("""
            SELECT * FROM wrong_book
            WHERE student_id = :studentId AND mastered = 0
            ORDER BY last_wrong_at DESC
            """)
    Flux<WrongBook> findUnmasteredByStudent(Long studentId);
}
