package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.UserFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedbackEntity, Long> {
}
