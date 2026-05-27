package in.maheshshelakee.moneymanager.dto;

import in.maheshshelakee.moneymanager.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private UserStatus status;
    private in.maheshshelakee.moneymanager.entity.Role role;
    private Boolean isVerified;
}
