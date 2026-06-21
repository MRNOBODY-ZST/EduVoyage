package cn.edu.shmtu.eduvoyage.interaction.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Mongo discussion document. A root post has {@code parentId == null}; replies
 * point to the parent post/reply and increment the direct parent's reply count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("discussion")
@CompoundIndexes({
        @CompoundIndex(name = "idx_discussion_course_ts", def = "{'courseId': 1, 'ts': -1}"),
        @CompoundIndex(name = "idx_discussion_parent_ts", def = "{'parentId': 1, 'ts': 1}")
})
public class Discussion {

    @Id
    private String id;

    @Indexed
    private Long courseId;

    @Indexed
    private Long nodeId;

    @Indexed
    private Long authorId;

    private String title;
    private String content;
    private String parentId;

    @Builder.Default
    private Set<Long> likes = new HashSet<>();

    private int replyCount;
    private Instant ts;
    private boolean deleted;
}
