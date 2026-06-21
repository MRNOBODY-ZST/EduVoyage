package cn.edu.shmtu.eduvoyage.interaction.repository;

import cn.edu.shmtu.eduvoyage.interaction.domain.Discussion;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DiscussionRepository extends ReactiveMongoRepository<Discussion, String> {

    @Query("{ '_id': ?0, 'deleted': false }")
    Mono<Discussion> findActiveById(String id);

    @Query(value = "{ 'parentId': ?0, 'deleted': false }", sort = "{ 'ts': 1 }")
    Flux<Discussion> findReplies(String parentId);
}
