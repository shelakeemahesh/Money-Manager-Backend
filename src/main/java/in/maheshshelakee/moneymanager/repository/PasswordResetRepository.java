package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.PasswordResetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordResetEntity, Long> {
    Optional<PasswordResetEntity> findByToken(String token);
    void deleteByEmail(String email);
}
