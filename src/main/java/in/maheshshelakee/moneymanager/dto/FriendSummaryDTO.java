package in.maheshshelakee.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendSummaryDTO {
    private String friendName;
    private BigDecimal totalGiven;
    private BigDecimal totalTaken;
    private BigDecimal netBalance;
    private List<FriendTransactionResponseDTO> pendingTransactions;
    private List<FriendTransactionResponseDTO> allTransactions;
}
