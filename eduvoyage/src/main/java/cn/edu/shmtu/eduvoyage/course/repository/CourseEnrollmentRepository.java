package cn.edu.shmtu.eduvoyage.course.repository;

import cn.edu.shmtu.eduvoyage.course.domain.CourseEnrollment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CourseEnrollmentRepository extends ReactiveCrudRepository<CourseEnrollment, Long> {

    @Query("SELECT * FROM course_enrollment WHERE course_id = :courseId AND student_id = :studentId AND deleted = 0")
    Mono<CourseEnrollment> findByCourseAndStudent(Long courseId, Long studentId);

    @Query("SELECT COUNT(*) FROM course_enrollment WHERE course_id = :courseId AND status = 1 AND deleted = 0")
    Mono<Long> countActiveByCourse(Long courseId);
}
