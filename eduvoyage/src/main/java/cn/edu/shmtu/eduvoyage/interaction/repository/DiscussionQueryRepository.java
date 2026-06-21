package cn.edu.shmtu.eduvoyage.interaction.repository;

import cn.edu.shmtu.eduvoyage.interaction.domain.Discussion;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DiscussionQueryRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public DiscussionQueryRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Flux<Discussion> pageRootPosts(Long courseId, Long nodeId, int offset, int limit) {
        Query query = baseRootQuery(courseId, nodeId)
                .with(Sort.by(Sort.Direction.DESC, "ts"))
                .skip(offset)
                .limit(limit);
        return mongoTemplate.find(query, Discussion.class);
    }

    public Mono<Long> countRootPosts(Long courseId, Long nodeId) {
        return mongoTemplate.count(baseRootQuery(courseId, nodeId), Discussion.class);
    }

    private static Query baseRootQuery(Long courseId, Long nodeId) {
        Criteria criteria = Criteria.where("courseId").is(courseId)
                .and("parentId").is(null)
                .and("deleted").is(false);
        if (nodeId != null) {
            criteria = criteria.and("nodeId").is(nodeId);
        }
        return Query.query(criteria);
    }
}
