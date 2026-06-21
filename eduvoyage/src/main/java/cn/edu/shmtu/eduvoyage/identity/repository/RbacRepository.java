package cn.edu.shmtu.eduvoyage.identity.repository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Hand-written RBAC queries over the composite-key join tables
 * ({@code sys_user_role}, {@code sys_role_permission}) and the cross-table
 * aggregations that don't fit a single-entity {@code ReactiveCrudRepository}.
 *
 * <p>Uses {@link DatabaseClient} directly so we keep full control over the SQL
 * (joins, {@code IN} clauses, batch inserts) while staying fully reactive.</p>
 */
@Repository
public class RbacRepository {

    private final DatabaseClient db;

    public RbacRepository(DatabaseClient db) {
        this.db = db;
    }

    /** Role codes granted to a user (e.g. {@code TEACHER}). */
    public Flux<String> findRoleCodesByUserId(Long userId) {
        return db.sql("""
                        SELECT r.code
                        FROM sys_user_role ur
                        JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
                        WHERE ur.user_id = :uid
                        """)
                .bind("uid", userId)
                .map((row, meta) -> row.get("code", String.class))
                .all();
    }

    /** Role ids granted to a user. */
    public Flux<Long> findRoleIdsByUserId(Long userId) {
        return db.sql("SELECT role_id FROM sys_user_role WHERE user_id = :uid")
                .bind("uid", userId)
                .map((row, meta) -> row.get("role_id", Long.class))
                .all();
    }

    /**
     * Distinct permission codes a user holds, resolved through their roles
     * (e.g. {@code course:create}). Drives both the access token's {@code perms}
     * claim and the frontend's dynamic menu.
     */
    public Flux<String> findPermissionCodesByUserId(Long userId) {
        return db.sql("""
                        SELECT DISTINCT p.code
                        FROM sys_user_role ur
                        JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                        JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
                        WHERE ur.user_id = :uid
                        """)
                .bind("uid", userId)
                .map((row, meta) -> row.get("code", String.class))
                .all();
    }

    /** Permission codes attached to a single role. */
    public Flux<String> findPermissionCodesByRoleId(Long roleId) {
        return db.sql("""
                        SELECT p.code
                        FROM sys_role_permission rp
                        JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
                        WHERE rp.role_id = :rid
                        """)
                .bind("rid", roleId)
                .map((row, meta) -> row.get("code", String.class))
                .all();
    }

    public Mono<Void> deleteUserRoles(Long userId) {
        return db.sql("DELETE FROM sys_user_role WHERE user_id = :uid")
                .bind("uid", userId)
                .then();
    }

    public Mono<Void> insertUserRole(Long userId, Long roleId) {
        return db.sql("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (:uid, :rid)")
                .bind("uid", userId)
                .bind("rid", roleId)
                .then();
    }

    /** Replaces a user's role set in one logical operation. */
    public Mono<Void> replaceUserRoles(Long userId, Collection<Long> roleIds) {
        Mono<Void> clear = deleteUserRoles(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return clear;
        }
        return clear.thenMany(Flux.fromIterable(roleIds)
                        .flatMap(rid -> insertUserRole(userId, rid)))
                .then();
    }

    public Mono<Void> deleteRolePermissions(Long roleId) {
        return db.sql("DELETE FROM sys_role_permission WHERE role_id = :rid")
                .bind("rid", roleId)
                .then();
    }

    public Mono<Void> insertRolePermission(Long roleId, Long permissionId) {
        return db.sql("INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (:rid, :pid)")
                .bind("rid", roleId)
                .bind("pid", permissionId)
                .then();
    }

    /** Replaces a role's permission set in one logical operation. */
    public Mono<Void> replaceRolePermissions(Long roleId, Collection<Long> permissionIds) {
        Mono<Void> clear = deleteRolePermissions(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return clear;
        }
        return clear.thenMany(Flux.fromIterable(permissionIds)
                        .flatMap(pid -> insertRolePermission(roleId, pid)))
                .then();
    }
}
