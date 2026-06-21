package cn.edu.shmtu.eduvoyage.drive.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Deduplicated physical object. Multiple {@link DriveNode file nodes} can point
 * to one row via identical sha256; {@code ref_count} tracks logical references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("drive_file")
public class DriveFile {

    @Id
    private Long id;

    private String sha256;
    private Long size;
    private String mime;
    private String bucket;

    @Column("object_key")
    private String objectKey;

    @Column("ref_count")
    private Integer refCount;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}
