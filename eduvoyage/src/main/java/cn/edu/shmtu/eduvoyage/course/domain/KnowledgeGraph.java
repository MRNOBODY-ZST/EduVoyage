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
 * {@code knowledge_graph} — the per-course container for knowledge nodes/edges.
 * The course module auto-provisions one default graph per course so knowledge
 * points always have a home; the dedicated knowledge-graph module (next phase)
 * layers edges and graph algorithms on top.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_graph")
public class KnowledgeGraph {

    @Id
    private Long id;

    @Column("course_id")
    private Long courseId;

    private String name;
    private Integer version;

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
