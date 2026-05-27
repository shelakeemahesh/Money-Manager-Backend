package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.AILogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AILogRepository extends JpaRepository<AILogEntity, Long> {
    Page<AILogEntity> findAllByOrderByTimestampDesc(Pageable pageable);
}
