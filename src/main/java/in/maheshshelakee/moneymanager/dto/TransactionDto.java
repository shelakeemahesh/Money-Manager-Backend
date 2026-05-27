package in.maheshshelakee.moneymanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private String type; // INCOME / EXPENSE
    private String title;
    private Double amount;
    private String category;
    private String note;
    private LocalDate date;
    private String paymentMethod;
    private Boolean flagged;
    private Long userId;
    private String userName;
    private String userEmail;
}
