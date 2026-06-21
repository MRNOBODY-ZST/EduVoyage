package cn.edu.shmtu.eduvoyage.interaction.repository;

import cn.edu.shmtu.eduvoyage.interaction.domain.Notification;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {

    @Query("{ '_id': ?0, 'deleted': false }")
    Mono<Notification> findActiveById(String id);

    Mono<Long> countByToUserIdAndReadFalseAndDeletedFalse(Long toUserId);
}
