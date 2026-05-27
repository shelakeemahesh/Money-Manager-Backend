package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.PaymentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistoryEntity, Long> {
    List<PaymentHistoryEntity> findByUserIdOrderByPaymentDateDesc(Long userId);
}
