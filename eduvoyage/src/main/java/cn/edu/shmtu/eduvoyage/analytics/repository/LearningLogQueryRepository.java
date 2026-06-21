package cn.edu.shmtu.eduvoyage.analytics.repository;

import cn.edu.shmtu.eduvoyage.analytics.domain.LearningLog;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class LearningLogQueryRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public LearningLogQueryRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<Long> sumDuration(Long userId, Long courseId, Instant from) {
        Criteria criteria = Criteria.where("userId").is(userId).and("ts").gte(from);
        if (courseId != null) {
            criteria = criteria.and("courseId").is(courseId);
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum("durationSec").as("total"));
        return mongoTemplate.aggregate(aggregation, "learning_log", Document.class)
                .next()
                .map(doc -> number(doc.get("total")))
                .defaultIfEmpty(0L);
    }

    public Mono<Long> sumCourseDuration(Long courseId, Instant from) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("courseId").is(courseId).and("ts").gte(from)),
                Aggregation.group().sum("durationSec").as("total"));
        return mongoTemplate.aggregate(aggregation, "learning_log", Document.class)
                .next()
                .map(doc -> number(doc.get("total")))
                .defaultIfEmpty(0L);
    }

    public Mono<Long> activeUsers(Long courseId, Instant from) {
        Criteria criteria = Criteria.where("ts").gte(from);
        if (courseId != null) {
            criteria = criteria.and("courseId").is(courseId);
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("userId"));
        return mongoTemplate.aggregate(aggregation, "learning_log", Document.class).count();
    }

    public Flux<LearningLog> recentByUser(Long userId, int limit) {
        Query query = Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "ts"))
                .limit(limit);
        return mongoTemplate.find(query, LearningLog.class);
    }

    public Flux<LearningLog> recentByUserSince(Long userId, Instant from) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("ts").gte(from))
                .with(Sort.by(Sort.Direction.DESC, "ts"));
        return mongoTemplate.find(query, LearningLog.class);
    }

    private static long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }
}
