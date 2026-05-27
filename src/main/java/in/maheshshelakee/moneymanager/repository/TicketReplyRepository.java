package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.TicketReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketReplyRepository extends JpaRepository<TicketReplyEntity, Long> {
    List<TicketReplyEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
