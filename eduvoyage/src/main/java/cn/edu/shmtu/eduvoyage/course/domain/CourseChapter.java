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

import java.time.LocalDateTime;

/**
 * {@code course_chapter} — a node in a course's chapter tree. {@code parentId}
 * is {@code 0} for a top-level chapter; otherwise it references another chapter's
 * id. Ordering within a level is by {@code sortNo} then id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("course_chapter")
public class CourseChapter {

    /** Sentinel parent id for a top-level chapter. */
    public static final long ROOT_PARENT = 0L;

    @Id
    private Long id;

    @Column("course_id")
    private Long courseId;

    @Column("parent_id")
    private Long parentId;

    private String title;

    @Column("sort_no")
    private Integer sortNo;

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
