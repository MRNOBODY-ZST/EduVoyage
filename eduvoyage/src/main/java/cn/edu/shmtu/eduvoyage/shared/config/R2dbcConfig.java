package cn.edu.shmtu.eduvoyage.shared.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * R2DBC wiring: enables reactive repositories, auditing callbacks
 * ({@code @CreatedDate}/{@code @LastModifiedDate}) and a reactive transaction
 * manager. The {@code ConnectionFactory} itself is auto-configured from
 * {@code spring.r2dbc.*}.
 */
@Configuration(proxyBeanMethods = false)
@EnableR2dbcRepositories(basePackages = "cn.edu.shmtu.eduvoyage")
@EnableR2dbcAuditing
public class R2dbcConfig {

    @Bean
    public ReactiveTransactionManager r2dbcTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
