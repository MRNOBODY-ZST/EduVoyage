package cn.edu.shmtu.eduvoyage.shared.security;

import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}: round-trips, type discrimination and
 * tamper/expiry rejection. No Spring context or infrastructure required.
 */
class JwtServiceTest {

    private final JwtService jwtService = new JwtService(properties(
            "unit-test-secret-key-that-is-definitely-32-bytes-long"));

    private static EduVoyageProperties properties(String secret) {
        EduVoyageProperties.Jwt jwt = new EduVoyageProperties.Jwt(
                secret, "eduvoyage", Duration.ofMinutes(15), Duration.ofDays(7));
        EduVoyageProperties.Security security = new EduVoyageProperties.Security(
                jwt, java.util.List.of(),
                new EduVoyageProperties.Login(5, Duration.ofMinutes(15)), "multi");
        return new EduVoyageProperties(security, null, null, null);
    }

    @Test
    void accessTokenRoundTripsRolesAndPermissions() {
        AuthUser user = new AuthUser(42L, "alice",
                Set.of("TEACHER"), Set.of("course:create", "course:update"));

        String token = jwtService.generateAccessToken(user);
        JwtPayload payload = jwtService.parse(token);

        assertThat(payload.userId()).isEqualTo(42L);
        assertThat(payload.username()).isEqualTo("alice");
        assertThat(payload.type()).isEqualTo(JwtService.TYPE_ACCESS);
        assertThat(payload.roles()).containsExactly("TEACHER");
        assertThat(payload.perms()).containsExactlyInAnyOrder("course:create", "course:update");
    }

    @Test
    void refreshTokenCarriesJtiAndRefreshType() {
        JwtService.RefreshToken refresh = jwtService.generateRefreshToken(7L, "bob");
        JwtPayload payload = jwtService.parse(refresh.token());

        assertThat(payload.userId()).isEqualTo(7L);
        assertThat(payload.type()).isEqualTo(JwtService.TYPE_REFRESH);
        assertThat(payload.jti()).isEqualTo(refresh.jti());
        assertThat(refresh.ttlSeconds()).isEqualTo(jwtService.refreshTtlSeconds());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(
                new AuthUser(1L, "x", Set.of("STUDENT"), Set.of()));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(BizErrorCode.TOKEN_INVALID);
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService other = new JwtService(properties(
                "a-completely-different-secret-key-also-32-bytes!!"));
        String foreign = other.generateAccessToken(
                new AuthUser(1L, "x", Set.of("STUDENT"), Set.of()));

        assertThatThrownBy(() -> jwtService.parse(foreign))
                .isInstanceOf(BizException.class);
    }
}
