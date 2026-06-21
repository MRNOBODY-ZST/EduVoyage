package cn.edu.shmtu.eduvoyage.identity.repository;

import cn.edu.shmtu.eduvoyage.identity.domain.SysUser;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for {@link SysUser}. Only non-deleted rows are considered by
 * the custom finders; physical deletes are never performed (logical delete via
 * the {@code deleted} flag).
 */
public interface SysUserRepository extends ReactiveCrudRepository<SysUser, Long> {

    @Query("SELECT * FROM sys_user WHERE username = :username AND deleted = 0")
    Mono<SysUser> findByUsername(String username);

    @Query("SELECT * FROM sys_user WHERE email = :email AND deleted = 0")
    Mono<SysUser> findByEmail(String email);

    @Query("SELECT * FROM sys_user WHERE phone = :phone AND deleted = 0")
    Mono<SysUser> findByPhone(String phone);

    @Query("SELECT * FROM sys_user WHERE id = :id AND deleted = 0")
    Mono<SysUser> findActiveById(Long id);

    @Query("SELECT COUNT(*) FROM sys_user WHERE username = :username AND deleted = 0")
    Mono<Long> countByUsername(String username);

    @Query("SELECT COUNT(*) FROM sys_user WHERE email = :email AND deleted = 0")
    Mono<Long> countByEmail(String email);

    @Query("SELECT COUNT(*) FROM sys_user WHERE phone = :phone AND deleted = 0")
    Mono<Long> countByPhone(String phone);
}
