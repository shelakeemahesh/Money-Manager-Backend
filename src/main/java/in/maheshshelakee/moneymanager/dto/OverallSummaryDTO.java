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
public class OverallSummaryDTO {
    private BigDecimal totalGiven;
    private BigDecimal totalTaken;
    private BigDecimal netBalance;
    private List<FriendBalanceItem> friendBalances;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendBalanceItem {
        private String friendName;
        private BigDecimal totalGiven;
        private BigDecimal totalTaken;
        private BigDecimal netBalance;
    }
}
