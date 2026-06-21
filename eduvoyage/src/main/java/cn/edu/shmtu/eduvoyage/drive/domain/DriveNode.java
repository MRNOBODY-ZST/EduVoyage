package cn.edu.shmtu.eduvoyage.drive.domain;

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
 * Logical node in the netdisk tree. Directories and files share the same tree;
 * file nodes reference a deduplicated physical {@link DriveFile}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("drive_node")
public class DriveNode {

    /** Personal drive space. */
    public static final int SPACE_PERSONAL = 1;
    /** Course shared drive space. */
    public static final int SPACE_COURSE = 2;

    public static final int FILE = 0;
    public static final int DIRECTORY = 1;

    @Id
    private Long id;

    @Column("owner_id")
    private Long ownerId;

    @Column("space_type")
    private Integer spaceType;

    @Column("course_id")
    private Long courseId;

    @Column("parent_id")
    private Long parentId;

    private String name;

    @Column("is_dir")
    private Integer isDir;

    @Column("file_id")
    private Long fileId;

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

    public boolean directory() {
        return isDir != null && isDir == DIRECTORY;
    }

    public boolean file() {
        return isDir == null || isDir == FILE;
    }
}
