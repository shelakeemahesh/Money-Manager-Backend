package in.maheshshelakee.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendStatsResponse {
    private Double totalSpent;
    private List<FriendSpendAggregate> friendBreakdown;
    private Map<String, Double> categoryBreakdown;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FriendSpendAggregate {
        private String friendName;
        private Double totalAmount;
    }
}
