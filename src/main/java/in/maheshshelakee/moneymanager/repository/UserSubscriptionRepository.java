package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.UserSubscriptionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, Long> {
    Optional<UserSubscriptionEntity> findByUserId(Long userId);
    Page<UserSubscriptionEntity> findAll(Pageable pageable);
}
