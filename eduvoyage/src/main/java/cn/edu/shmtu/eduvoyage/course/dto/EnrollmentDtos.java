package cn.edu.shmtu.eduvoyage.course.dto;

import cn.edu.shmtu.eduvoyage.course.domain.CourseEnrollment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Enrollment view and the student's learning-progress update. */
public final class EnrollmentDtos {

    private EnrollmentDtos() {
    }

    @Schema(description = "学习进度更新请求")
    public record ProgressRequest(
            @Schema(description = "进度百分比 0-100", example = "42.5")
            @NotNull(message = "进度不能为空")
            @DecimalMin(value = "0.0", message = "进度不能小于 0")
            @DecimalMax(value = "100.0", message = "进度不能大于 100")
            BigDecimal progress
    ) {
    }

    @Schema(description = "选课信息")
    public record EnrollmentResponse(
            @Schema(description = "选课 id") Long id,
            @Schema(description = "课程 id") Long courseId,
            @Schema(description = "学生 id") Long studentId,
            @Schema(description = "状态 1已选0已退") Integer status,
            @Schema(description = "进度百分比") BigDecimal progress,
            @Schema(description = "选课时间") LocalDateTime enrolledAt
    ) {
        public static EnrollmentResponse from(CourseEnrollment e) {
            return new EnrollmentResponse(e.getId(), e.getCourseId(), e.getStudentId(),
                    e.getStatus(), e.getProgress(), e.getEnrolledAt());
        }
    }
}
