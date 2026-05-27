package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByTransactionId(String transactionId);
    List<SubscriptionEntity> findByUserIdOrderBySubmittedAtDesc(Long userId);
    boolean existsByUserIdAndPaymentStatus(Long userId, String paymentStatus);
    List<SubscriptionEntity> findByPaymentStatusOrderBySubmittedAtDesc(String paymentStatus);
}
