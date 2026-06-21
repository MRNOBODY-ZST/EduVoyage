package cn.edu.shmtu.eduvoyage.identity.repository;

import cn.edu.shmtu.eduvoyage.identity.domain.SysPermission;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SysPermissionRepository extends ReactiveCrudRepository<SysPermission, Long> {

    @Query("SELECT * FROM sys_permission WHERE deleted = 0 ORDER BY id")
    Flux<SysPermission> findAllActive();
}
