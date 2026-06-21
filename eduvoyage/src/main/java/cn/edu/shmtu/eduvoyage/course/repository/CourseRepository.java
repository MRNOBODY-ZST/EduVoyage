package cn.edu.shmtu.eduvoyage.course.repository;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Single-entity access for {@code course}. Dynamic catalog search (optional
 * filters + paging) lives in {@link CourseQueryRepository}.
 */
public interface CourseRepository extends ReactiveCrudRepository<Course, Long> {

    @Query("SELECT * FROM course WHERE id = :id AND deleted = 0")
    Mono<Course> findActiveById(Long id);
}
