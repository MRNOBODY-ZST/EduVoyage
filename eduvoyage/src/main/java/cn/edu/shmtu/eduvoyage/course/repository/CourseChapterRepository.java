package cn.edu.shmtu.eduvoyage.course.repository;

import cn.edu.shmtu.eduvoyage.course.domain.CourseChapter;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CourseChapterRepository extends ReactiveCrudRepository<CourseChapter, Long> {

    @Query("SELECT * FROM course_chapter WHERE id = :id AND deleted = 0")
    Mono<CourseChapter> findActiveById(Long id);

    @Query("SELECT * FROM course_chapter WHERE course_id = :courseId AND deleted = 0 ORDER BY sort_no, id")
    Flux<CourseChapter> findByCourseId(Long courseId);

    @Query("SELECT * FROM course_chapter WHERE parent_id = :parentId AND deleted = 0 ORDER BY sort_no, id")
    Flux<CourseChapter> findByParentId(Long parentId);

    @Query("SELECT COUNT(*) FROM course_chapter WHERE parent_id = :parentId AND deleted = 0")
    Mono<Long> countByParentId(Long parentId);
}
