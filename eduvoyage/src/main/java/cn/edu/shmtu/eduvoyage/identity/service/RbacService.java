package cn.edu.shmtu.eduvoyage.identity.service;

import cn.edu.shmtu.eduvoyage.identity.domain.SysUser;
import cn.edu.shmtu.eduvoyage.identity.repository.RbacRepository;
import cn.edu.shmtu.eduvoyage.shared.security.AuthUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves a user's effective RBAC view (role codes + permission codes) and
 * assembles the {@link AuthUser} principal embedded in access tokens. Centralised
 * here so login, refresh, and {@code /me} all compute authorities identically.
 */
@Service
public class RbacService {

    private final RbacRepository rbacRepository;

    public RbacService(RbacRepository rbacRepository) {
        this.rbacRepository = rbacRepository;
    }

    /** Role codes for a user. */
    public Mono<List<String>> rolesOf(Long userId) {
        return rbacRepository.findRoleCodesByUserId(userId).collectList();
    }

    /** Distinct permission codes for a user (resolved via roles). */
    public Mono<List<String>> permissionsOf(Long userId) {
        return rbacRepository.findPermissionCodesByUserId(userId).collectList();
    }

    /** Roles and permissions resolved together. */
    public Mono<Tuple2<List<String>, List<String>>> rolesAndPermissions(Long userId) {
        return Mono.zip(rolesOf(userId), permissionsOf(userId));
    }

    /** Builds the {@link AuthUser} principal carried in the access token. */
    public Mono<AuthUser> toAuthUser(SysUser user) {
        return rolesAndPermissions(user.getId())
                .map(tuple -> {
                    Set<String> roles = new LinkedHashSet<>(tuple.getT1());
                    Set<String> perms = new LinkedHashSet<>(tuple.getT2());
                    return new AuthUser(user.getId(), user.getUsername(), roles, perms);
                });
    }
}
