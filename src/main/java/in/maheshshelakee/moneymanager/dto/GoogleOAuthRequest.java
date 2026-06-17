package in.maheshshelakee.moneymanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleOAuthRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
