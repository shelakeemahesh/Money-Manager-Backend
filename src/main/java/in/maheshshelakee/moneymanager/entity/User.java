package in.maheshshelakee.moneymanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_users", indexes = {
        @Index(name = "idx_user_phone_number", columnList = "phone_number")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    
    @Column(name = "profile_image")
    private String profileImage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    private Boolean isActive;
    private String otpCode;
    private LocalDateTime otpExpiry;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private Boolean isVerified;
    
    private LocalDateTime lastLogin;

    @PrePersist
    void prePersist() {
        if (isActive == null) isActive = false;
        if (role == null) role = Role.USER;
        if (status == null) status = UserStatus.ACTIVE;
        if (isVerified == null) isVerified = false;
    }
}
