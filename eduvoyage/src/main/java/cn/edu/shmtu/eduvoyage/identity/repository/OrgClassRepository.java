package cn.edu.shmtu.eduvoyage.identity.repository;

import cn.edu.shmtu.eduvoyage.identity.domain.OrgClass;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrgClassRepository extends ReactiveCrudRepository<OrgClass, Long> {

    @Query("SELECT * FROM org_class WHERE deleted = 0 ORDER BY id")
    Flux<OrgClass> findAllActive();

    @Query("SELECT * FROM org_class WHERE major_id = :majorId AND deleted = 0 ORDER BY id")
    Flux<OrgClass> findByMajorId(Long majorId);

    @Query("SELECT * FROM org_class WHERE id = :id AND deleted = 0")
    Mono<OrgClass> findActiveById(Long id);
}
