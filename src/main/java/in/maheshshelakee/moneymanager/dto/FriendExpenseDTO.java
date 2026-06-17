package in.maheshshelakee.moneymanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendExpenseDTO {
    private Long id;

    @NotBlank(message = "Friend's name is required")
    private String friendName;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "Category is required")
    private String category;

    private String description;

    @NotNull(message = "Date is required")
    private LocalDate expenseDate;
}
