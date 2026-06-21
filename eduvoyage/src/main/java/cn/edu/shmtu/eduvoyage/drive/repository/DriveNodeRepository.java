package cn.edu.shmtu.eduvoyage.drive.repository;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveNode;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DriveNodeRepository extends ReactiveCrudRepository<DriveNode, Long> {

    @Query("SELECT * FROM drive_node WHERE id = :id AND deleted = 0")
    Mono<DriveNode> findActiveById(Long id);

    @Query("""
            SELECT * FROM drive_node
            WHERE parent_id = :parentId AND deleted = 0
            ORDER BY is_dir DESC, name ASC, id ASC
            """)
    Flux<DriveNode> findActiveChildren(Long parentId);

    @Query("""
            SELECT * FROM drive_node
            WHERE owner_id = :ownerId AND space_type = 1 AND parent_id = 0 AND deleted = 0
            ORDER BY is_dir DESC, name ASC, id ASC
            """)
    Flux<DriveNode> findPersonalRootChildren(Long ownerId);

    @Query("""
            SELECT * FROM drive_node
            WHERE space_type = 2 AND course_id = :courseId AND parent_id = 0 AND deleted = 0
            ORDER BY is_dir DESC, name ASC, id ASC
            """)
    Flux<DriveNode> findCourseRootChildren(Long courseId);
}
