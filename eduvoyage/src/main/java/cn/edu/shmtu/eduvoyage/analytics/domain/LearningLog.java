package cn.edu.shmtu.eduvoyage.analytics.domain;

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

/**
 * High-frequency learning behavior log. The TTL index keeps only the recent
 * 180-day analysis window in MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("learning_log")
@CompoundIndexes({
        @CompoundIndex(name = "idx_learning_user_ts", def = "{'userId': 1, 'ts': -1}"),
        @CompoundIndex(name = "idx_learning_course_ts", def = "{'courseId': 1, 'ts': -1}")
})
public class LearningLog {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private Long courseId;

    @Indexed
    private Long nodeId;

    private String action;
    private Integer durationSec;

    @Indexed(expireAfter = "180d")
    private Instant ts;
}
