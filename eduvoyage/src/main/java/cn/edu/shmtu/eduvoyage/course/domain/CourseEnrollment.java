package cn.edu.shmtu.eduvoyage.course.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code course_enrollment} — a student's enrollment in a course, carrying their
 * learning {@code progress} (0–100). Unique per (course, student); re-enrolling a
 * previously-dropped record reactivates it rather than inserting a duplicate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("course_enrollment")
public class CourseEnrollment {

    /** Active enrollment. */
    public static final int STATUS_ENROLLED = 1;
    /** Student dropped the course (kept for history). */
    public static final int STATUS_DROPPED = 0;

    @Id
    private Long id;

    @Column("course_id")
    private Long courseId;

    @Column("student_id")
    private Long studentId;

    private Integer status;
    private BigDecimal progress;

    @Column("enrolled_at")
    private LocalDateTime enrolledAt;

    @CreatedBy
    @Column("created_by")
    private Long createdBy;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    private Integer deleted;
}
