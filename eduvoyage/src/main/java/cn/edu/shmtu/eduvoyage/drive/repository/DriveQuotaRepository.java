package cn.edu.shmtu.eduvoyage.drive.repository;

import cn.edu.shmtu.eduvoyage.drive.domain.DriveQuota;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface DriveQuotaRepository extends ReactiveCrudRepository<DriveQuota, Long> {
}
