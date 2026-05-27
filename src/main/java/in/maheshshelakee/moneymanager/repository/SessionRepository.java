package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
    Optional<SessionEntity> findByToken(String token);
    List<SessionEntity> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    void deleteByToken(String token);
}
