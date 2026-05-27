package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.SupportTicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {
    Page<SupportTicketEntity> findAll(Pageable pageable);
}
