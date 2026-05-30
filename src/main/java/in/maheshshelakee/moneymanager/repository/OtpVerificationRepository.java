package in.maheshshelakee.moneymanager.repository;

import in.maheshshelakee.moneymanager.entity.OtpVerification;
import in.maheshshelakee.moneymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByUserAndOtpCodeAndIsUsedFalse(User user, String otpCode);

    Optional<OtpVerification> findByUserAndIsUsedFalseAndExpiresAtAfter(User user, LocalDateTime expiresAt);

    void deleteByUser(User user);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime since);
}
