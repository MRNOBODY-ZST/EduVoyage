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

/**
 * In-app notification / message document. SSE uses the same stored document as
 * the delivery payload, so reconnecting clients can always recover from Mongo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("notification")
@CompoundIndexes({
        @CompoundIndex(name = "idx_notification_user_read_ts", def = "{'toUserId': 1, 'read': 1, 'ts': -1}"),
        @CompoundIndex(name = "idx_notification_user_category_ts", def = "{'toUserId': 1, 'category': 1, 'ts': -1}")
})
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long toUserId;

    private String type;
    private String title;
    private String body;
    private String refId;
    private String category;
    private boolean read;
    private Instant ts;
    private boolean deleted;
}
