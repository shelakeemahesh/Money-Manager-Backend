package in.maheshshelakee.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusDTO {
    private String status; // "PENDING", "APPROVED", "REJECTED", "NONE"
    private String planType; // "MONTHLY", "YEARLY", null
    private Double amount;
    private String transactionId;
    private LocalDateTime submittedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime expiryDate;
    private Long remainingDays;
}
