package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.FraudEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraudEventRepository extends JpaRepository<FraudEventEntity, Long> {
    Optional<FraudEventEntity> findByTransactionId(Long transactionId);
    Page<FraudEventEntity> findAll(Pageable pageable);
}
