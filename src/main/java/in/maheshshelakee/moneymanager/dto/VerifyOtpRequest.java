package in.maheshshelakee.moneymanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Email or Phone Number is required")
    private String emailOrPhone;

    @NotBlank(message = "OTP Code is required")
    private String otpCode;
}
