package in.maheshshelakee.moneymanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email or Phone Number is required")
    private String emailOrPhone;

    @NotBlank(message = "Password is required")
    private String password;
}
