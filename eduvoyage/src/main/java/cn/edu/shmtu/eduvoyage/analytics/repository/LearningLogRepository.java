package cn.edu.shmtu.eduvoyage.analytics.repository;

import cn.edu.shmtu.eduvoyage.analytics.domain.LearningLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface LearningLogRepository extends ReactiveMongoRepository<LearningLog, String> {
}
