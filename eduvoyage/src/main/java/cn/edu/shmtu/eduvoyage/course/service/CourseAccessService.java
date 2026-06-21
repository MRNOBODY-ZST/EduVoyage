package cn.edu.shmtu.eduvoyage.course.service;

import cn.edu.shmtu.eduvoyage.course.domain.Course;
import cn.edu.shmtu.eduvoyage.course.domain.CourseEnrollment;
import cn.edu.shmtu.eduvoyage.course.repository.CourseEnrollmentRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRelationRepository;
import cn.edu.shmtu.eduvoyage.course.repository.CourseRepository;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Shared course-access guard for modules whose content is visible only to course
 * teachers/co-teachers/admins or actively enrolled students.
 */
@Service
public class CourseAccessService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final CourseRepository courseRepository;
    private final CourseRelationRepository relationRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public CourseAccessService(CourseRepository courseRepository,
                               CourseRelationRepository relationRepository,
                               CourseEnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.relationRepository = relationRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public Mono<Course> requireParticipant(Long courseId, AuthUser user) {
        if (user == null) {
            return Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED));
        }
        return courseRepository.findActiveById(courseId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "课程不存在")))
                .flatMap(course -> {
                    if (user.hasRole(ROLE_ADMIN) || course.getTeacherId().equals(user.id())) {
                        return Mono.just(course);
                    }
                    return relationRepository.isCourseTeacher(courseId, user.id())
                            .flatMap(teacher -> teacher
                                    ? Mono.just(course)
                                    : enrollmentRepository.findByCourseAndStudent(courseId, user.id())
                                    .filter(enrollment -> enrollment.getStatus() != null
                                            && enrollment.getStatus() == CourseEnrollment.STATUS_ENROLLED)
                                    .switchIfEmpty(Mono.error(new BizException(BizErrorCode.ACCESS_DENIED,
                                            "无权访问该课程内容")))
                                    .thenReturn(course));
                });
    }
}
