package in.maheshshelakee.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDto {
    private Long id;
    private Long userId;
    private String userName;
    private String category;
    private Double budgetedAmount;
    private Double spentAmount;
    private Double remainingAmount;
    private String period;
    private String status;
}
