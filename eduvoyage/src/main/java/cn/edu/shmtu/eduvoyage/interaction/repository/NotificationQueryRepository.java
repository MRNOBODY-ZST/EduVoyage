package cn.edu.shmtu.eduvoyage.interaction.repository;

import cn.edu.shmtu.eduvoyage.interaction.domain.Notification;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class NotificationQueryRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public NotificationQueryRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Flux<Notification> page(Long toUserId, Boolean read, String category, int offset, int limit) {
        Query query = baseQuery(toUserId, read, category)
                .with(Sort.by(Sort.Direction.DESC, "ts"))
                .skip(offset)
                .limit(limit);
        return mongoTemplate.find(query, Notification.class);
    }

    public Mono<Long> count(Long toUserId, Boolean read, String category) {
        return mongoTemplate.count(baseQuery(toUserId, read, category), Notification.class);
    }

    public Mono<Long> markAllRead(Long toUserId, String category) {
        Query query = baseQuery(toUserId, false, category);
        return mongoTemplate.updateMulti(query, Update.update("read", true), Notification.class)
                .map(result -> result.getModifiedCount());
    }

    private static Query baseQuery(Long toUserId, Boolean read, String category) {
        Criteria criteria = Criteria.where("toUserId").is(toUserId).and("deleted").is(false);
        if (read != null) {
            criteria = criteria.and("read").is(read);
        }
        if (category != null && !category.isBlank()) {
            criteria = criteria.and("category").is(category.trim());
        }
        return Query.query(criteria);
    }
}
