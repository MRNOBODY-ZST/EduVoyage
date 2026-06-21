package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.identity.domain.SysUser;
import cn.edu.shmtu.eduvoyage.identity.dto.LoginRequest;
import cn.edu.shmtu.eduvoyage.identity.repository.RbacRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.SysRoleRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.SysUserRepository;
import cn.edu.shmtu.eduvoyage.shared.config.EduVoyageProperties;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import cn.edu.shmtu.eduvoyage.shared.security.JwtPayload;
import cn.edu.shmtu.eduvoyage.shared.security.JwtService;
import cn.edu.shmtu.eduvoyage.shared.security.RefreshTokenStore;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} — login flow and refresh-token rotation —
 * with every collaborator mocked. No Spring context, DB, or Redis required; the
 * reactive contracts are asserted with {@link StepVerifier}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock SysUserRepository userRepository;
    @Mock SysRoleRepository roleRepository;
    @Mock RbacRepository rbacRepository;
    @Mock CaptchaService captchaService;
    @Mock LoginAttemptService loginAttemptService;
    @Mock RefreshTokenStore refreshTokenStore;
    @Mock R2dbcEntityTemplate entityTemplate;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(properties("multi"));
    private final IdGenerator idGenerator = new IdGenerator(1L);

    private AuthService authService;

    private static final String RAW_PASSWORD = "Admin@123";
    private SysUser activeUser;

    private static EduVoyageProperties properties(String concurrentStrategy) {
        EduVoyageProperties.Jwt jwt = new EduVoyageProperties.Jwt(
                "unit-test-secret-key-that-is-definitely-32-bytes-long",
                "eduvoyage", Duration.ofMinutes(15), Duration.ofDays(7));
        EduVoyageProperties.Security security = new EduVoyageProperties.Security(
                jwt, List.of(),
                new EduVoyageProperties.Login(5, Duration.ofMinutes(15)), concurrentStrategy);
        return new EduVoyageProperties(security, null, null, null);
    }

    @BeforeEach
    void setUp() {
        RbacService rbacService = new RbacService(rbacRepository);
        authService = new AuthService(userRepository, roleRepository, rbacRepository, rbacService,
                captchaService, loginAttemptService, jwtService, refreshTokenStore,
                passwordEncoder, idGenerator, entityTemplate, properties("multi"));

        activeUser = SysUser.builder()
                .id(100L)
                .username("admin")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .realName("管理员")
                .status(SysUser.STATUS_ACTIVE)
                .deleted(0)
                .build();
    }

    @Test
    void loginSucceedsAndIssuesTokenPair() {
        LoginRequest req = new LoginRequest("admin", RAW_PASSWORD, null, null);

        when(captchaService.verify(null, null)).thenReturn(Mono.just(true));
        when(loginAttemptService.isLocked("admin")).thenReturn(Mono.just(false));
        when(userRepository.findByUsername("admin")).thenReturn(Mono.just(activeUser));
        when(loginAttemptService.reset("admin")).thenReturn(Mono.just(true));
        when(userRepository.save(any(SysUser.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(rbacRepository.findRoleCodesByUserId(100L)).thenReturn(Flux.just("ADMIN"));
        when(rbacRepository.findPermissionCodesByUserId(100L))
                .thenReturn(Flux.just("user:read", "user:create"));
        when(refreshTokenStore.store(eq(100L), anyString(), anyLong())).thenReturn(Mono.just(true));

        StepVerifier.create(authService.login(req))
                .assertNext(token -> {
                    assertThat(token.accessToken()).isNotBlank();
                    assertThat(token.refreshToken()).isNotBlank();
                    assertThat(token.tokenType()).isEqualTo("Bearer");
                    assertThat(token.expiresIn()).isEqualTo(jwtService.accessTtlSeconds());
                    assertThat(token.user()).isNotNull();
                    assertThat(token.user().username()).isEqualTo("admin");

                    JwtPayload access = jwtService.parse(token.accessToken());
                    assertThat(access.type()).isEqualTo(JwtService.TYPE_ACCESS);
                    assertThat(access.roles()).contains("ADMIN");
                    assertThat(access.perms()).contains("user:read", "user:create");

                    JwtPayload refresh = jwtService.parse(token.refreshToken());
                    assertThat(refresh.type()).isEqualTo(JwtService.TYPE_REFRESH);
                })
                .verifyComplete();

        verify(loginAttemptService).reset("admin");
        verify(refreshTokenStore).store(eq(100L), anyString(), anyLong());
    }

    @Test
    void loginWithWrongPasswordRecordsFailureAndReturnsBadCredentials() {
        LoginRequest req = new LoginRequest("admin", "wrong-password", null, null);

        when(captchaService.verify(null, null)).thenReturn(Mono.just(true));
        when(loginAttemptService.isLocked("admin")).thenReturn(Mono.just(false));
        when(userRepository.findByUsername("admin")).thenReturn(Mono.just(activeUser));
        when(loginAttemptService.recordFailure("admin")).thenReturn(Mono.just(4L));

        StepVerifier.create(authService.login(req))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.BAD_CREDENTIALS))
                .verify();

        verify(loginAttemptService).recordFailure("admin");
        verify(refreshTokenStore, never()).store(anyLong(), anyString(), anyLong());
    }

    @Test
    void loginOnLockedAccountIsRejectedBeforePasswordCheck() {
        LoginRequest req = new LoginRequest("admin", RAW_PASSWORD, null, null);

        when(captchaService.verify(null, null)).thenReturn(Mono.just(true));
        when(loginAttemptService.isLocked("admin")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.login(req))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.ACCOUNT_LOCKED))
                .verify();

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void loginUnknownUserCountsAsFailureWithGenericError() {
        LoginRequest req = new LoginRequest("ghost", RAW_PASSWORD, null, null);

        when(captchaService.verify(null, null)).thenReturn(Mono.just(true));
        when(loginAttemptService.isLocked("ghost")).thenReturn(Mono.just(false));
        when(userRepository.findByUsername("ghost")).thenReturn(Mono.empty());
        when(loginAttemptService.recordFailure("ghost")).thenReturn(Mono.just(4L));

        StepVerifier.create(authService.login(req))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.BAD_CREDENTIALS))
                .verify();

        verify(loginAttemptService).recordFailure("ghost");
    }

    @Test
    void refreshRotatesTokenRevokingOldJti() {
        JwtService.RefreshToken issued = jwtService.generateRefreshToken(100L, "admin");

        when(refreshTokenStore.isValid(100L, issued.jti())).thenReturn(Mono.just(true));
        when(refreshTokenStore.revoke(100L, issued.jti())).thenReturn(Mono.just(true));
        when(userRepository.findActiveById(100L)).thenReturn(Mono.just(activeUser));
        when(rbacRepository.findRoleCodesByUserId(100L)).thenReturn(Flux.just("ADMIN"));
        when(rbacRepository.findPermissionCodesByUserId(100L)).thenReturn(Flux.just("user:read"));
        when(refreshTokenStore.store(eq(100L), anyString(), anyLong())).thenReturn(Mono.just(true));

        StepVerifier.create(authService.refresh(issued.token()))
                .assertNext(token -> {
                    JwtPayload newRefresh = jwtService.parse(token.refreshToken());
                    assertThat(newRefresh.jti()).isNotEqualTo(issued.jti());
                })
                .verifyComplete();

        verify(refreshTokenStore).revoke(100L, issued.jti());
        verify(refreshTokenStore, times(1)).store(eq(100L), anyString(), anyLong());
    }

    @Test
    void refreshWithRevokedTokenIsRejected() {
        JwtService.RefreshToken issued = jwtService.generateRefreshToken(100L, "admin");

        when(refreshTokenStore.isValid(100L, issued.jti())).thenReturn(Mono.just(false));
        // findActiveById is assembled into the .then() chain but, because validation
        // errors first, must never be subscribed; stub it so assembly doesn't NPE.
        lenient().when(userRepository.findActiveById(100L)).thenReturn(Mono.just(activeUser));

        StepVerifier.create(authService.refresh(issued.token()))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.REFRESH_TOKEN_INVALID))
                .verify();

        // a revoked token is never rotated: no new jti stored, old jti not revoked
        verify(refreshTokenStore, never()).revoke(anyLong(), anyString());
        verify(refreshTokenStore, never()).store(anyLong(), anyString(), anyLong());
    }

    @Test
    void refreshRejectsAnAccessTokenPresentedAsRefresh() {
        // an access token carries type=access; refresh() must reject it outright
        String accessToken = jwtService.generateAccessToken(
                new AuthUser(100L, "admin", Set.of("ADMIN"), Set.of("user:read")));

        StepVerifier.create(authService.refresh(accessToken))
                .expectErrorSatisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(BizErrorCode.REFRESH_TOKEN_INVALID))
                .verify();

        verify(refreshTokenStore, never()).isValid(anyLong(), anyString());
    }
}
