package in.maheshshelakee.moneymanager.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketReplyDto {
    private Long id;
    private Long ticketId;
    private String senderEmail;
    private String senderName;
    private String senderRole;
    private String message;
    private LocalDateTime createdAt;
}
