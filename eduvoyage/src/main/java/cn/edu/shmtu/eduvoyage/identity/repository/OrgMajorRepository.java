package cn.edu.shmtu.eduvoyage.identity.repository;

import cn.edu.shmtu.eduvoyage.identity.domain.OrgMajor;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrgMajorRepository extends ReactiveCrudRepository<OrgMajor, Long> {

    @Query("SELECT * FROM org_major WHERE deleted = 0 ORDER BY id")
    Flux<OrgMajor> findAllActive();

    @Query("SELECT * FROM org_major WHERE department_id = :departmentId AND deleted = 0 ORDER BY id")
    Flux<OrgMajor> findByDepartmentId(Long departmentId);

    @Query("SELECT * FROM org_major WHERE id = :id AND deleted = 0")
    Mono<OrgMajor> findActiveById(Long id);
}
