package in.maheshshelakee.moneymanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category type is required")
    @Pattern(regexp = "^(INCOME|EXPENSE)$", message = "Type must be 'INCOME' or 'EXPENSE'")
    private String type;

    @Builder.Default
    private String icon = "📁";

    @Builder.Default
    private String color = "#6366f1";

    private Boolean globalTemplate;
}
