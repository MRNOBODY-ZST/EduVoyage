package cn.edu.shmtu.eduvoyage.identity.repository;

import cn.edu.shmtu.eduvoyage.identity.domain.OrgDepartment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrgDepartmentRepository extends ReactiveCrudRepository<OrgDepartment, Long> {

    @Query("SELECT * FROM org_department WHERE deleted = 0 ORDER BY id")
    Flux<OrgDepartment> findAllActive();

    @Query("SELECT * FROM org_department WHERE id = :id AND deleted = 0")
    Mono<OrgDepartment> findActiveById(Long id);
}
