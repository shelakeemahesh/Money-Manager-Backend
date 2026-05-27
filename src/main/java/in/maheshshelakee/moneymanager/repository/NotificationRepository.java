package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<NotificationEntity> findByStatusAndScheduledAtBefore(String status, LocalDateTime time);
}
