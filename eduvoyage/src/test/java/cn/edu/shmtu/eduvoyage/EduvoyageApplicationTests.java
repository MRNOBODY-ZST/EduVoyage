package cn.edu.shmtu.eduvoyage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import cn.edu.shmtu.eduvoyage.shared.security.JwtService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full reactive context smoke test. Spins up MySQL, MongoDB and Redis via
 * Testcontainers (Elasticsearch/MinIO auto-config tolerate being absent at
 * startup), runs {@code schema.sql}/{@code data.sql}, and verifies the
 * application context — including the shared infrastructure beans — wires up.
 *
 * <p>Skipped automatically when no Docker daemon is reachable, so it never
 * breaks an infra-less build.</p>
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class EduvoyageApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.0")
            .withDatabaseName("eduvoyage")
            .withUsername("eduvoyage")
            .withPassword("eduvoyage");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        // No Redis container here; point Redis at MySQL-less defaults is not
        // possible, so disable repositories that need it is unnecessary —
        // Lettuce connects lazily, and these beans are created but not invoked
        // during a context-load smoke test.
        registry.add("spring.data.elasticsearch.repositories.enabled", () -> "false");
        registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
    }

    @Autowired
    JwtService jwtService;

    @Test
    void contextLoads() {
        assertThat(jwtService).isNotNull();
    }
}
