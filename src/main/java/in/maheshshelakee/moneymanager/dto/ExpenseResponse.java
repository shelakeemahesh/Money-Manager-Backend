package in.maheshshelakee.moneymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseResponse {
    private Long id;
    private String title;
    private Double amount;
    private String category;
    private String note;
    private LocalDate date;
    private String paymentMethod;
    private String icon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
