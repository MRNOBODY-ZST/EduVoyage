package cn.edu.shmtu.eduvoyage.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * MongoDB wiring: reactive repositories + auditing. The
 * {@code ReactiveMongoTemplate} / connection is auto-configured from
 * {@code spring.data.mongodb.*}. TTL and compound indexes are declared on the
 * document classes via {@code @Indexed}/{@code @CompoundIndex} (auto index
 * creation is enabled in dev, applied explicitly in prod).
 */
@Configuration(proxyBeanMethods = false)
@EnableReactiveMongoRepositories(basePackages = "cn.edu.shmtu.eduvoyage")
@EnableReactiveMongoAuditing
public class MongoConfig {
}
