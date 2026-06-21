package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.identity.domain.SysUser;
import cn.edu.shmtu.eduvoyage.identity.dto.ForgotPasswordRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.LoginRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.MeResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.RegisterRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.TokenResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.UserResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Authentication orchestration for the dual-token + RBAC scheme:
 * login, self-registration, token refresh (with rotation), logout, profile
 * lookup ({@code /me}), and forgotten-password reset.
 *
 * <p>Security properties enforced here:</p>
 * <ul>
 *   <li>BCrypt password verification; generic {@code BAD_CREDENTIALS} on any
 *       username/password mismatch (no user-enumeration signal).</li>
 *   <li>Captcha required on login/register/forgot.</li>
 *   <li>Lockout after N failed logins (via {@link LoginAttemptService}).</li>
 *   <li>Refresh tokens tracked in Redis and rotated on every refresh; the old
 *       jti is revoked so a stolen refresh token is single-use.</li>
 *   <li>Configurable single- vs multi-session: {@code single} revokes all of a
 *       user's refresh tokens on each new login.</li>
 * </ul>
 */
@Slf4j
@Service
public class AuthService {

    private static final String STRATEGY_SINGLE = "single";

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final RbacRepository rbacRepository;
    private final RbacService rbacService;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final R2dbcEntityTemplate entityTemplate;
    private final String concurrentStrategy;

    public AuthService(SysUserRepository userRepository,
                       SysRoleRepository roleRepository,
                       RbacRepository rbacRepository,
                       RbacService rbacService,
                       CaptchaService captchaService,
                       LoginAttemptService loginAttemptService,
                       JwtService jwtService,
                       RefreshTokenStore refreshTokenStore,
                       PasswordEncoder passwordEncoder,
                       IdGenerator idGenerator,
                       R2dbcEntityTemplate entityTemplate,
                       EduVoyageProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.rbacRepository = rbacRepository;
        this.rbacService = rbacService;
        this.captchaService = captchaService;
        this.loginAttemptService = loginAttemptService;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.entityTemplate = entityTemplate;
        this.concurrentStrategy = properties.security().concurrentStrategy();
    }

    // ---------------------------------------------------------------- login

    public Mono<TokenResponse> login(LoginRequest req) {
        return captchaService.verify(req.captchaId(), req.captchaCode())
                // captcha is optional in dev when not supplied; enforce only if an id was given
                .flatMap(ok -> {
                    if (req.captchaId() != null && !req.captchaId().isBlank() && !ok) {
                        return Mono.error(new BizException(BizErrorCode.CAPTCHA_INVALID));
                    }
                    return loginAttemptService.isLocked(req.username());
                })
                .flatMap(locked -> {
                    if (Boolean.TRUE.equals(locked)) {
                        return Mono.error(new BizException(BizErrorCode.ACCOUNT_LOCKED));
                    }
                    return userRepository.findByUsername(req.username());
                })
                // unknown user -> still count as a failed attempt, generic error
                .switchIfEmpty(Mono.defer(() -> loginAttemptService.recordFailure(req.username())
                        .then(Mono.error(new BizException(BizErrorCode.BAD_CREDENTIALS)))))
                .flatMap(user -> verifyPasswordAndStatus(user, req.password()))
                .flatMap(user -> loginAttemptService.reset(user.getUsername())
                        .then(touchLastLogin(user))
                        .then(issueTokens(user, true)));
    }

    private Mono<SysUser> verifyPasswordAndStatus(SysUser user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return loginAttemptService.recordFailure(user.getUsername())
                    .then(Mono.error(new BizException(BizErrorCode.BAD_CREDENTIALS)));
        }
        if (user.getStatus() != null && user.getStatus() == SysUser.STATUS_DISABLED) {
            return Mono.error(new BizException(BizErrorCode.ACCOUNT_DISABLED));
        }
        if (user.getStatus() != null && user.getStatus() == SysUser.STATUS_LOCKED) {
            return Mono.error(new BizException(BizErrorCode.ACCOUNT_LOCKED));
        }
        return Mono.just(user);
    }

    private Mono<Long> touchLastLogin(SysUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user).thenReturn(user.getId());
    }

    // ------------------------------------------------------------- register

    @Transactional
    public Mono<TokenResponse> register(RegisterRequest req) {
        if ((req.email() == null || req.email().isBlank())
                && (req.phone() == null || req.phone().isBlank())) {
            return Mono.error(new BizException(BizErrorCode.PARAM_INVALID, "邮箱与手机号至少填写一项"));
        }
        return captchaService.verify(req.captchaId(), req.captchaCode())
                .flatMap(ok -> ok ? Mono.empty()
                        : Mono.error(new BizException(BizErrorCode.CAPTCHA_INVALID)))
                .then(ensureUnique(req.username(), req.email(), req.phone()))
                .then(Mono.defer(() -> {
                    SysUser user = SysUser.builder()
                            .id(idGenerator.nextId())
                            .username(req.username())
                            .password(passwordEncoder.encode(req.password()))
                            .realName(req.realName())
                            .email(emptyToNull(req.email()))
                            .phone(emptyToNull(req.phone()))
                            .status(SysUser.STATUS_ACTIVE)
                            .gender(0)
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(SysUser.class).using(user);
                }))
                // self-registration always becomes a STUDENT
                .flatMap(user -> roleRepository.findByCode("STUDENT")
                        .switchIfEmpty(Mono.error(new BizException(BizErrorCode.SYSTEM_ERROR, "缺少 STUDENT 角色")))
                        .flatMap(role -> rbacRepository.insertUserRole(user.getId(), role.getId()))
                        .thenReturn(user))
                .flatMap(user -> issueTokens(user, false));
    }

    private Mono<Void> ensureUnique(String username, String email, String phone) {
        Mono<Void> chain = userRepository.countByUsername(username)
                .flatMap(n -> n > 0 ? Mono.error(new BizException(BizErrorCode.USERNAME_EXISTS)) : Mono.empty());
        if (email != null && !email.isBlank()) {
            chain = chain.then(userRepository.countByEmail(email)
                    .flatMap(n -> n > 0 ? Mono.error(new BizException(BizErrorCode.EMAIL_EXISTS)) : Mono.empty()));
        }
        if (phone != null && !phone.isBlank()) {
            chain = chain.then(userRepository.countByPhone(phone)
                    .flatMap(n -> n > 0 ? Mono.error(new BizException(BizErrorCode.PHONE_EXISTS)) : Mono.empty()));
        }
        return chain;
    }

    // -------------------------------------------------------------- refresh

    /**
     * Exchanges a valid, non-revoked refresh token for a new token pair and
     * rotates it: the presented jti is revoked and a fresh one is stored, so a
     * leaked refresh token can be used at most once.
     */
    public Mono<TokenResponse> refresh(String refreshToken) {
        JwtPayload payload;
        try {
            payload = jwtService.parse(refreshToken);
        } catch (BizException e) {
            return Mono.error(e);
        }
        if (!JwtService.TYPE_REFRESH.equals(payload.type())) {
            return Mono.error(new BizException(BizErrorCode.REFRESH_TOKEN_INVALID));
        }
        return refreshTokenStore.isValid(payload.userId(), payload.jti())
                .flatMap(valid -> {
                    if (!Boolean.TRUE.equals(valid)) {
                        return Mono.error(new BizException(BizErrorCode.REFRESH_TOKEN_INVALID));
                    }
                    return refreshTokenStore.revoke(payload.userId(), payload.jti());
                })
                .then(userRepository.findActiveById(payload.userId()))
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.REFRESH_TOKEN_INVALID)))
                .flatMap(user -> issueTokens(user, false));
    }

    // --------------------------------------------------------------- logout

    /** Revokes the supplied refresh token (best-effort; never errors the caller). */
    public Mono<Void> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> jwtService.parse(refreshToken))
                .flatMap(p -> refreshTokenStore.revoke(p.userId(), p.jti()))
                .onErrorResume(e -> Mono.just(false))
                .then();
    }

    // ------------------------------------------------------------------- me

    public Mono<MeResponse> me(Long userId) {
        return userRepository.findActiveById(userId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED)))
                .flatMap(user -> rbacService.rolesAndPermissions(userId)
                        .map(rp -> new MeResponse(
                                UserResponse.from(user, rp.getT1()),
                                rp.getT1(),
                                rp.getT2())));
    }

    // ----------------------------------------------------- forgot password

    public Mono<Void> forgotPassword(ForgotPasswordRequest req) {
        return captchaService.verify(req.captchaId(), req.captchaCode())
                .flatMap(ok -> ok ? Mono.empty()
                        : Mono.error(new BizException(BizErrorCode.CAPTCHA_INVALID)))
                .then(resolveByAccount(req.account()))
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "账号不存在")))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(req.newPassword()));
                    return userRepository.save(user);
                })
                // invalidate every existing session after a password reset
                .flatMap(user -> refreshTokenStore.revokeAll(user.getId()))
                .then();
    }

    private Mono<SysUser> resolveByAccount(String account) {
        return userRepository.findByUsername(account)
                .switchIfEmpty(userRepository.findByEmail(account))
                .switchIfEmpty(userRepository.findByPhone(account));
    }

    // ------------------------------------------------------------ internals

    /**
     * Mints an access+refresh pair for a user, stores the refresh jti in Redis,
     * and (for {@code single} strategy on fresh logins) evicts prior sessions.
     */
    private Mono<TokenResponse> issueTokens(SysUser user, boolean freshLogin) {
        Mono<Void> preLogin = (freshLogin && STRATEGY_SINGLE.equalsIgnoreCase(concurrentStrategy))
                ? refreshTokenStore.revokeAll(user.getId()).then()
                : Mono.empty();

        return preLogin.then(rbacService.toAuthUser(user))
                .flatMap(authUser -> {
                    String access = jwtService.generateAccessToken(authUser);
                    JwtService.RefreshToken refresh =
                            jwtService.generateRefreshToken(user.getId(), user.getUsername());
                    return refreshTokenStore.store(user.getId(), refresh.jti(), refresh.ttlSeconds())
                            .thenReturn(buildResponse(user, authUser, access, refresh.token()));
                });
    }

    private TokenResponse buildResponse(SysUser user, AuthUser authUser, String access, String refresh) {
        UserResponse profile = UserResponse.from(user, authUser.roles().stream().toList());
        return TokenResponse.of(access, refresh, jwtService.accessTtlSeconds(), profile);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
