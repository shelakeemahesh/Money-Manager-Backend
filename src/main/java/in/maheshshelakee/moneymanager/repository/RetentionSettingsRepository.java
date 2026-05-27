package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.RetentionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetentionSettingsRepository extends JpaRepository<RetentionSettings, Long> {
}
