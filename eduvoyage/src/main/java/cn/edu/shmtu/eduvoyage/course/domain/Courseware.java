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
 * {@code courseware} — a learning resource attached to a knowledge node: a video,
 * document, rich-text page, external link, etc. Binary payloads live in the drive
 * module (referenced by {@code fileId}); rich-text/markdown bodies are stored in
 * MongoDB and referenced by {@code contentRef}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("courseware")
public class Courseware {

    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_DOCUMENT = 2;
    public static final int TYPE_RICHTEXT = 3;
    public static final int TYPE_LINK = 4;

    @Id
    private Long id;

    @Column("node_id")
    private Long nodeId;

    private String title;
    private Integer type;

    /** MongoDB document id for rich-text/markdown bodies, or external URL for links. */
    @Column("content_ref")
    private String contentRef;

    /** Drive file id for binary payloads (video/document). */
    @Column("file_id")
    private Long fileId;

    @Column("duration_sec")
    private Integer durationSec;

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
