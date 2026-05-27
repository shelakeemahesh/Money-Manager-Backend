package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.BackupHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {
}
