package cn.edu.shmtu.eduvoyage.drive.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Per-user logical storage quota. Course-space uploads are charged to the
 * uploader because the schema stores quotas by user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("drive_quota")
public class DriveQuota {

    @Id
    @Column("user_id")
    private Long userId;

    @Column("total_bytes")
    private Long totalBytes;

    @Column("used_bytes")
    private Long usedBytes;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
