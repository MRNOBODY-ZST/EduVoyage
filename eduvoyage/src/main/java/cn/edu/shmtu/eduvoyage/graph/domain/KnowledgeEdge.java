package cn.edu.shmtu.eduvoyage.graph.domain;

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
 * {@code knowledge_edge} — a directed relation between two knowledge nodes inside
 * one graph. {@code from_id} points to {@code to_id}; for a
 * {@link #TYPE_PREREQUISITE} edge this reads "from is a prerequisite of to", so the
 * set of prerequisite edges must stay acyclic (enforced by the service). A
 * {@link #TYPE_RELATED} edge is a soft cross-link and does not constrain ordering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_edge")
public class KnowledgeEdge {

    /** {@code from} must be learned before {@code to}; participates in DAG checks. */
    public static final String TYPE_PREREQUISITE = "PREREQUISITE";
    /** Soft association with no ordering constraint. */
    public static final String TYPE_RELATED = "RELATED";

    @Id
    private Long id;

    @Column("graph_id")
    private Long graphId;

    @Column("from_id")
    private Long fromId;

    @Column("to_id")
    private Long toId;

    private String type;
    private Double weight;

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
