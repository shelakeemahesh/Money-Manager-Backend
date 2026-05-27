package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.BackupSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupSettingsRepository extends JpaRepository<BackupSettings, Long> {
}
