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
 * Public share entry guarded by an opaque token and optional extraction code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("drive_share")
public class DriveShare {

    @Id
    private Long id;

    @Column("node_id")
    private Long nodeId;

    @Column("owner_id")
    private Long ownerId;

    private String token;

    @Column("extract_code")
    private String extractCode;

    @Column("expire_at")
    private LocalDateTime expireAt;

    @Column("view_count")
    private Integer viewCount;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    private Integer deleted;
}
