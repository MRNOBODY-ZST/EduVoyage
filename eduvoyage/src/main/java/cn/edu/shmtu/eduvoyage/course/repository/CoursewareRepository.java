package cn.edu.shmtu.eduvoyage.course.repository;

import cn.edu.shmtu.eduvoyage.course.domain.Courseware;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CoursewareRepository extends ReactiveCrudRepository<Courseware, Long> {

    @Query("SELECT * FROM courseware WHERE id = :id AND deleted = 0")
    Mono<Courseware> findActiveById(Long id);

    @Query("SELECT * FROM courseware WHERE node_id = :nodeId AND deleted = 0 ORDER BY sort_no, id")
    Flux<Courseware> findByNodeId(Long nodeId);
}
