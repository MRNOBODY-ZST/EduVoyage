package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.identity.domain.SysUser;
import cn.edu.shmtu.eduvoyage.identity.dto.ChangePasswordRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.ProfileUpdateRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.UserCreateRequest;
import cn.edu.shmtu.eduvoyage.identity.dto.UserResponse;
import cn.edu.shmtu.eduvoyage.identity.dto.UserUpdateRequest;
import cn.edu.shmtu.eduvoyage.identity.repository.RbacRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.SysRoleRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.SysUserQueryRepository;
import cn.edu.shmtu.eduvoyage.identity.repository.SysUserRepository;
import cn.edu.shmtu.eduvoyage.shared.api.PageResult;
import cn.edu.shmtu.eduvoyage.shared.exception.BizErrorCode;
import cn.edu.shmtu.eduvoyage.shared.exception.BizException;
import cn.edu.shmtu.eduvoyage.shared.security.RefreshTokenStore;
import cn.edu.shmtu.eduvoyage.shared.util.IdGenerator;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * User management: admin CRUD (with role assignment), the user's own profile
 * maintenance, and password change. New rows are written with
 * {@link R2dbcEntityTemplate#insert} because ids are application-generated
 * Snowflakes ({@code save()} would issue an UPDATE for a non-null id).
 */
@Service
public class UserService {

    private final SysUserRepository userRepository;
    private final SysUserQueryRepository userQueryRepository;
    private final SysRoleRepository roleRepository;
    private final RbacRepository rbacRepository;
    private final R2dbcEntityTemplate entityTemplate;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final RefreshTokenStore refreshTokenStore;

    public UserService(SysUserRepository userRepository,
                       SysUserQueryRepository userQueryRepository,
                       SysRoleRepository roleRepository,
                       RbacRepository rbacRepository,
                       R2dbcEntityTemplate entityTemplate,
                       PasswordEncoder passwordEncoder,
                       IdGenerator idGenerator,
                       RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.userQueryRepository = userQueryRepository;
        this.roleRepository = roleRepository;
        this.rbacRepository = rbacRepository;
        this.entityTemplate = entityTemplate;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.refreshTokenStore = refreshTokenStore;
    }

    // ------------------------------------------------------------- queries

    public Mono<PageResult<UserResponse>> page(String keyword, Integer status, Long classId,
                                               int pageNo, int pageSize) {
        int safeNo = Math.max(1, pageNo);
        int safeSize = Math.min(Math.max(1, pageSize), 200);
        int offset = (safeNo - 1) * safeSize;

        Mono<List<UserResponse>> rows = userQueryRepository.search(keyword, status, classId, offset, safeSize)
                .flatMap(this::withRoles)
                .collectList();
        Mono<Long> total = userQueryRepository.count(keyword, status, classId);

        return Mono.zip(rows, total)
                .map(t -> PageResult.of(t.getT1(), t.getT2(), safeNo, safeSize));
    }

    public Mono<UserResponse> get(Long id) {
        return userRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "用户不存在")))
                .flatMap(this::withRoles);
    }

    private Mono<UserResponse> withRoles(SysUser user) {
        return rbacRepository.findRoleCodesByUserId(user.getId())
                .collectList()
                .map(roles -> UserResponse.from(user, roles));
    }

    // -------------------------------------------------------------- create

    @Transactional
    public Mono<UserResponse> create(UserCreateRequest req) {
        return ensureUnique(req.username(), req.email(), req.phone())
                .then(resolveRoleIds(req.roleCodes()))
                .flatMap(roleIds -> {
                    SysUser user = SysUser.builder()
                            .id(idGenerator.nextId())
                            .username(req.username())
                            .password(passwordEncoder.encode(req.password()))
                            .realName(req.realName())
                            .email(emptyToNull(req.email()))
                            .phone(emptyToNull(req.phone()))
                            .gender(req.gender() == null ? 0 : req.gender())
                            .status(SysUser.STATUS_ACTIVE)
                            .classId(req.classId())
                            .deleted(0)
                            .build();
                    return entityTemplate.insert(SysUser.class).using(user)
                            .flatMap(saved -> rbacRepository.replaceUserRoles(saved.getId(), roleIds)
                                    .thenReturn(saved));
                })
                .flatMap(this::withRoles);
    }

    // -------------------------------------------------------------- update

    @Transactional
    public Mono<UserResponse> update(Long id, UserUpdateRequest req) {
        return userRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "用户不存在")))
                .flatMap(user -> {
                    if (req.realName() != null) {
                        user.setRealName(req.realName());
                    }
                    if (req.email() != null) {
                        user.setEmail(emptyToNull(req.email()));
                    }
                    if (req.phone() != null) {
                        user.setPhone(emptyToNull(req.phone()));
                    }
                    if (req.gender() != null) {
                        user.setGender(req.gender());
                    }
                    if (req.avatarUrl() != null) {
                        user.setAvatarUrl(req.avatarUrl());
                    }
                    if (req.status() != null) {
                        user.setStatus(req.status());
                    }
                    if (req.classId() != null) {
                        user.setClassId(req.classId());
                    }
                    Mono<Void> roleUpdate = req.roleCodes() == null ? Mono.empty()
                            : resolveRoleIds(req.roleCodes())
                            .flatMap(ids -> rbacRepository.replaceUserRoles(id, ids));
                    return userRepository.save(user).then(roleUpdate).thenReturn(user);
                })
                .flatMap(this::withRoles);
    }

    // -------------------------------------------------------------- delete

    /** Logical delete; also revokes the user's sessions. */
    public Mono<Void> delete(Long id, Long operatorId) {
        if (id.equals(operatorId)) {
            return Mono.error(new BizException(BizErrorCode.OPERATION_NOT_ALLOWED, "不能删除当前登录账号"));
        }
        return userRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "用户不存在")))
                .flatMap(user -> {
                    user.setDeleted(1);
                    return userRepository.save(user);
                })
                .then(refreshTokenStore.revokeAll(id))
                .then();
    }

    // ----------------------------------------------------------- self ops

    @Transactional
    public Mono<UserResponse> updateProfile(Long userId, ProfileUpdateRequest req) {
        return userRepository.findActiveById(userId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED)))
                .flatMap(user -> {
                    if (req.realName() != null) {
                        user.setRealName(req.realName());
                    }
                    if (req.email() != null) {
                        user.setEmail(emptyToNull(req.email()));
                    }
                    if (req.phone() != null) {
                        user.setPhone(emptyToNull(req.phone()));
                    }
                    if (req.gender() != null) {
                        user.setGender(req.gender());
                    }
                    if (req.avatarUrl() != null) {
                        user.setAvatarUrl(req.avatarUrl());
                    }
                    return userRepository.save(user);
                })
                .flatMap(this::withRoles);
    }

    public Mono<Void> changePassword(Long userId, ChangePasswordRequest req) {
        return userRepository.findActiveById(userId)
                .switchIfEmpty(Mono.error(new BizException(BizErrorCode.UNAUTHENTICATED)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
                        return Mono.error(new BizException(BizErrorCode.BAD_CREDENTIALS, "原密码错误"));
                    }
                    user.setPassword(passwordEncoder.encode(req.newPassword()));
                    return userRepository.save(user);
                })
                // force re-login on all devices after a password change
                .then(refreshTokenStore.revokeAll(userId))
                .then();
    }

    // ------------------------------------------------------------ helpers

    private Mono<List<Long>> resolveRoleIds(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Mono.just(List.of());
        }
        return Flux.fromIterable(roleCodes)
                .flatMap(code -> roleRepository.findByCode(code)
                        .switchIfEmpty(Mono.error(new BizException(
                                BizErrorCode.PARAM_INVALID, "角色不存在: " + code))))
                .map(role -> role.getId())
                .collectList();
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

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
