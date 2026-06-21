package cn.edu.shmtu.eduvoyage.assessment.repository;

import cn.edu.shmtu.eduvoyage.assessment.domain.Homework;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HomeworkRepository extends ReactiveCrudRepository<Homework, Long> {

    @Query("SELECT * FROM homework WHERE id = :id AND deleted = 0")
    Mono<Homework> findActiveById(Long id);

    @Query("SELECT * FROM homework WHERE course_id = :courseId AND deleted = 0 ORDER BY id DESC")
    Flux<Homework> findByCourseId(Long courseId);

    @Query("""
            SELECT * FROM homework
            WHERE course_id = :courseId AND status = :status AND deleted = 0
            ORDER BY id DESC
            """)
    Flux<Homework> findByCourseIdAndStatus(Long courseId, Integer status);
}
