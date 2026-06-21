package cn.edu.shmtu.eduvoyage.course.service;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.domain.CourseEnrollment;
import cn.edu.shmtu.eduvoyage.course.dto.EnrollmentDtos.EnrollmentResponse;
import cn.edu.shmtu.eduvoyage.course.repository.CourseEnrollmentRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRelationRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Student-facing enrollment: enroll/drop, learning-progress updates, and course
 * favorites. Enrollment is only allowed on a published course; re-enrolling a
 * previously-dropped record reactivates it rather than creating a duplicate
 * (the unique key is (course_id, student_id)).
 */
@Service
public class EnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseRelationRepository relationRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final IdGenerator idGenerator;

    public EnrollmentService(CourseEnrollmentRepository enrollmentRepository,
                             CourseRepository courseRepository,
                             CourseRelationRepository relationRepository,
                             R2dbcEntityTemplate entityTemplate,
                             IdGenerator idGenerator) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.relationRepository = relationRepository;
        this.entityTemplate = entityTemplate;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Mono<EnrollmentResponse> enroll(Long courseId, Long studentId) {
        return requirePublishedCourse(courseId)
                .then(enrollmentRepository.findByCourseAndStudent(courseId, studentId))
                .flatMap(existing -> {
                    if (existing.getStatus() != null && existing.getStatus() == CourseEnrollment.STATUS_ENROLLED) {
                        return Mono.error(new BizException(BizErrorCode.DATA_CONFLICT, "已选修该课程"));
                    }
                    existing.setStatus(CourseEnrollment.STATUS_ENROLLED);
                    return enrollmentRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    CourseEnrollment enrollment = CourseEnrollment.builder()
                            .id(idGenerator.nextId())
                            .courseId(courseId)
                            .studentId(studentId)
                            .status(CourseEnrollment.STATUS_ENROLLED)
                            .progress(BigDecimal.ZERO)
                            .build();
                    return entityTemplate.insert(CourseEnrollment.class).using(enrollment);
                }))
                .map(EnrollmentResponse::from);
    }

    @Transactional
    public Mono<Void> drop(Long courseId, Long studentId) {
        return requireEnrollment(courseId, studentId)
                .flatMap(enrollment -> {
                    enrollment.setStatus(CourseEnrollment.STATUS_DROPPED);
                    return enrollmentRepository.save(enrollment);
                })
                .then();
    }

    public Mono<EnrollmentResponse> updateProgress(Long courseId, Long studentId, BigDecimal progress) {
        return requireEnrollment(courseId, studentId)
                .flatMap(enrollment -> {
                    if (enrollment.getStatus() == null || enrollment.getStatus() != CourseEnrollment.STATUS_ENROLLED) {
                        return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "未选修该课程"));
                    }
                    enrollment.setProgress(progress);
                    return enrollmentRepository.save(enrollment);
                })
                .map(EnrollmentResponse::from);
    }

    public Mono<EnrollmentResponse> myEnrollment(Long courseId, Long studentId) {
        return requireEnrollment(courseId, studentId).map(EnrollmentResponse::from);
    }

    // ----------------------------------------------------------- favorites

    public Mono<Void> favorite(Long courseId, Long studentId) {
        return requireCourse(courseId).then(relationRepository.addFavorite(studentId, courseId));
    }

    public Mono<Void> unfavorite(Long courseId, Long studentId) {
        return relationRepository.removeFavorite(studentId, courseId);
    }

    // ------------------------------------------------------------ helpers

    private Mono<Course> requireCourse(Long courseId) {
        return courseRepository.findActiveById(courseId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "课程不存在")));
    }

    private Mono<Course> requirePublishedCourse(Long courseId) {
        return requireCourse(courseId)
                .flatMap(course -> course.getStatus() != null && course.getStatus() == Course.STATUS_PUBLISHED
                        ? Mono.just(course)
                        : Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "课程未发布，暂不可选")));
    }

    private Mono<CourseEnrollment> requireEnrollment(Long courseId, Long studentId) {
        return enrollmentRepository.findByCourseAndStudent(courseId, studentId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "未找到选课记录")));
    }
}
