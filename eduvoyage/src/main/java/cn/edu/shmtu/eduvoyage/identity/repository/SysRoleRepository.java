package cn.edu.shmtu.eduvoyage.identity.repository;

import cn.edu.shmtu.eduvoyage.identity.domain.SysRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SysRoleRepository extends ReactiveCrudRepository<SysRole, Long> {

    @Query("SELECT * FROM sys_role WHERE deleted = 0 ORDER BY id")
    Flux<SysRole> findAllActive();

    @Query("SELECT * FROM sys_role WHERE code = :code AND deleted = 0")
    Mono<SysRole> findByCode(String code);

    @Query("SELECT COUNT(*) FROM sys_role WHERE code = :code AND deleted = 0")
    Mono<Long> countByCode(String code);
}
