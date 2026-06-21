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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code course} — a course owned by a teacher. The {@code id} is an
 * application-generated Snowflake; new rows are written via
 * {@code R2dbcEntityTemplate.insert} (a non-null id makes {@code save()} update).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("course")
public class Course {

    /** {@code status}: editable draft, not visible to students. */
    public static final int STATUS_DRAFT = 0;
    /** {@code status}: published and open for enrollment/learning. */
    public static final int STATUS_PUBLISHED = 1;
    /** {@code status}: archived (read-only, hidden from catalog). */
    public static final int STATUS_ARCHIVED = 2;

    /** {@code visibility}: only enrolled students / scoped classes. */
    public static final int VISIBILITY_PRIVATE = 0;
    /** {@code visibility}: anyone in the institution may discover and enroll. */
    public static final int VISIBILITY_PUBLIC = 1;

    @Id
    private Long id;

    private String title;

    @Column("cover_url")
    private String coverUrl;

    private String intro;
    private BigDecimal credit;

    @Column("teacher_id")
    private Long teacherId;

    private Integer visibility;
    private Integer status;

    @Column("start_date")
    private LocalDate startDate;

    @Column("end_date")
    private LocalDate endDate;

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
