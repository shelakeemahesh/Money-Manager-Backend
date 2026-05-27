package in.maheshshelakee.moneymanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestDTO {

    @NotBlank(message = "Plan type is required")
    private String planType; // "MONTHLY" or "YEARLY"

    @NotBlank(message = "Transaction ID / UTR is required")
    private String transactionId;
}
