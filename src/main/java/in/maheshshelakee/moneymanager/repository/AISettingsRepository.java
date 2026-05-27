package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.AISettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AISettingsRepository extends JpaRepository<AISettingsEntity, Long> {
}
