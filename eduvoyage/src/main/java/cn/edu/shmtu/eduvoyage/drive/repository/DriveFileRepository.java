package cn.edu.shmtu.eduvoyage.drive.repository;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveFile;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface DriveFileRepository extends ReactiveCrudRepository<DriveFile, Long> {

    @Query("SELECT * FROM drive_file WHERE sha256 = :sha256")
    Mono<DriveFile> findBySha256(String sha256);
}
