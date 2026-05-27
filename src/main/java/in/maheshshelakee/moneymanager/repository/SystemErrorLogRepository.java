package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.SystemErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemErrorLogRepository extends JpaRepository<SystemErrorLog, Long> {
}
