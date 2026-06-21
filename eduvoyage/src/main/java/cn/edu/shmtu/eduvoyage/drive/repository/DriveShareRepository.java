package cn.edu.shmtu.eduvoyage.drive.repository;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveShare;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DriveShareRepository extends ReactiveCrudRepository<DriveShare, Long> {

    @Query("SELECT * FROM drive_share WHERE token = :token AND deleted = 0")
    Mono<DriveShare> findActiveByToken(String token);

    @Query("""
            SELECT * FROM drive_share
            WHERE owner_id = :ownerId AND deleted = 0
            ORDER BY created_at DESC, id DESC
            """)
    Flux<DriveShare> findActiveByOwnerId(Long ownerId);

    @Query("SELECT * FROM drive_share WHERE id = :id AND deleted = 0")
    Mono<DriveShare> findActiveById(Long id);
}
