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
 * {@code knowledge_node} — a knowledge point within a course. Always belongs to a
 * {@code graph} (the course's default graph unless re-assigned) and optionally to
 * a {@code chapter}. {@code posX}/{@code posY} carry layout coordinates for the
 * graph canvas; the knowledge-graph module manages edges between nodes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_node")
public class KnowledgeNode {

    @Id
    private Long id;

    @Column("course_id")
    private Long courseId;

    @Column("chapter_id")
    private Long chapterId;

    @Column("graph_id")
    private Long graphId;

    private String name;
    private String description;

    @Column("learn_goal")
    private String learnGoal;

    @Column("est_minutes")
    private Integer estMinutes;

    @Column("pos_x")
    private Double posX;

    @Column("pos_y")
    private Double posY;

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
