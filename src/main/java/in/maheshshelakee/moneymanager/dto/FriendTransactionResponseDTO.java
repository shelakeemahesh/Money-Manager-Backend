package in.maheshshelakee.moneymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.maheshshelakee.moneymanager.entity.FriendTransactionStatus;
import in.maheshshelakee.moneymanager.entity.FriendTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendTransactionResponseDTO {
    private Long id;
    private String friendName;
    private FriendTransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private LocalDate dueDate;
    private FriendTransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
