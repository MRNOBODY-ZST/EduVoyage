package cn.edu.shmtu.eduvoyage.identity;

import cn.edu.shmtu.eduvoyage.identity.dto.LoginRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.TokenResponse;
import cn.edu.shmtu.eduvoyage.identity.service.AuthService;
import cn.edu.shmtu.eduvoyage.identity.service.RbacService;
import cn.edu.shmtu.eduvoyage.identity.service.UserService;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.JwtPayload;
import cn.edu.shmtu.eduvoyage.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

/**
 * Integration tests for the identity module against real infrastructure:
 * MySQL (schema.sql + data.sql loaded via the dev profile's sql.init) and Redis
 * (lockout + refresh-token store). Exercises the wired Spring beans end-to-end —
 * no mocks — so the hand-written DatabaseClient RBAC queries, the
 * R2dbcEntityTemplate insert path, and BCrypt verification are all covered.
 *
 * <p>Auto-skips when no Docker daemon is reachable.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class IdentityIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.0")
            .withDatabaseName("eduvoyage")
            .withUsername("eduvoyage")
            .withPassword("eduvoyage");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // no Elasticsearch container in this slice
        registry.add("spring.data.elasticsearch.repositories.enabled", () -> "false");
        registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
    }

    @Autowired AuthService authService;
    @Autowired RbacService rbacService;
    @Autowired UserService userService;
    @Autowired JwtService jwtService;

    private static final long ADMIN_ID = 1L;

    /** Seeded admin logs in; access token must carry the ADMIN role + admin perms. */
    @Test
    void seededAdminLogsInWithFullAuthorities() {
        LoginRequest req = new LoginRequest("admin", "Admin@123", null, null);

        StepVerifier.create(authService.login(req))
                .assertNext(token -> {
                    JwtPayload access = jwtService.parse(token.accessToken());
                    org.assertj.core.api.Assertions.assertThat(access.userId()).isEqualTo(ADMIN_ID);
                    org.assertj.core.api.Assertions.assertThat(access.roles()).contains("ADMIN");
                    org.assertj.core.api.Assertions.assertThat(access.perms())
                            .contains("user:read", "user:create", "course:create");
                    org.assertj.core.api.Assertions.assertThat(token.user().username()).isEqualTo("admin");
                })
                .verifyComplete();
    }

    /** Wrong password yields a generic BAD_CREDENTIALS against the real BCrypt hash. */
    @Test
    void loginWithWrongPasswordIsRejected() {
        LoginRequest req = new LoginRequest("admin", "not-the-password", null, null);

        StepVerifier.create(authService.login(req))
                .expectErrorSatisfies(e -> org.assertj.core.api.Assertions
                        .assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.BAD_CREDENTIALS))
                .verify();
    }

    /** RBAC resolution via the hand-written join-table queries. */
    @Test
    void rbacResolvesSeededTeacherAuthorities() {
        long teacherId = 2L;
        StepVerifier.create(rbacService.rolesAndPermissions(teacherId))
                .assertNext(rp -> {
                    org.assertj.core.api.Assertions.assertThat(rp.getT1()).contains("TEACHER");
                    org.assertj.core.api.Assertions.assertThat(rp.getT2())
                            .contains("course:create", "homework:grade")
                            .doesNotContain("user:delete");
                })
                .verifyComplete();
    }

    /** A full login → refresh round-trip against the real Redis refresh-token store. */
    @Test
    void refreshRotatesAgainstRealRedis() {
        LoginRequest req = new LoginRequest("teacher", "Teacher@123", null, null);

        TokenResponse first = authService.login(req).block();
        org.assertj.core.api.Assertions.assertThat(first).isNotNull();

        StepVerifier.create(authService.refresh(first.refreshToken()))
                .assertNext(rotated -> {
                    JwtPayload oldRt = jwtService.parse(first.refreshToken());
                    JwtPayload newRt = jwtService.parse(rotated.refreshToken());
                    org.assertj.core.api.Assertions.assertThat(newRt.jti()).isNotEqualTo(oldRt.jti());
                })
                .verifyComplete();

        // the old refresh token was revoked on rotation; reusing it must fail
        StepVerifier.create(authService.refresh(first.refreshToken()))
                .expectErrorSatisfies(e -> org.assertj.core.api.Assertions
                        .assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.REFRESH_TOKEN_INVALID))
                .verify();
    }

    /** Paged user query hits the dynamic DatabaseClient search and returns seeds. */
    @Test
    void userPageReturnsSeededAccounts() {
        StepVerifier.create(userService.page(null, null, null, 1, 10))
                .assertNext(page -> {
                    org.assertj.core.api.Assertions.assertThat(page.total()).isGreaterThanOrEqualTo(3);
                    org.assertj.core.api.Assertions.assertThat(page.records())
                            .extracting(u -> u.username())
                            .contains("admin", "teacher", "student");
                })
                .verifyComplete();
    }
}
