package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.entity.OtpVerification;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepo;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${app.otp-expiration-minutes:10}")
    private int otpExpirationMinutes;

    private static final int MAX_OTP_PER_10_MINS = 3;

    @Transactional
    public void generateAndSend(User user, String deliveryType) {
        generateAndSend(user, deliveryType, "VERIFY");
    }

    @Transactional
    public void generateAndSend(User user, String deliveryType, String purpose) {
        // Rate limit check: count OTPs created in last 10 minutes
        long recentCount = otpRepo.countByUserAndCreatedAtAfter(
                user, LocalDateTime.now().minusMinutes(10)
        );
        if (recentCount >= MAX_OTP_PER_10_MINS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP requests. Try again later.");
        }

        // Delete any existing unused OTPs for this user
        otpRepo.deleteByUser(user);

        // Generate 6-digit OTP
        String rawOtp = String.format("%06d", new SecureRandom().nextInt(1000000));
        String hashedOtp = hashToken(rawOtp);

        // Save to DB
        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .otpCode(hashedOtp)
                .deliveryType(deliveryType != null ? deliveryType.toUpperCase() : "EMAIL")
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .isUsed(false)
                .build();
        otpRepo.save(otp);

        String subject;
        String body;
        if ("RESET".equalsIgnoreCase(purpose)) {
            subject = "Reset Password Code";
            body = "Hi " + user.getFullName() + ",\n\n"
                    + "You requested to reset your Money Manager password.\n\n"
                    + "Your 6-digit reset OTP code is:\n"
                    + rawOtp + "\n\n"
                    + "This OTP will expire in " + otpExpirationMinutes + " minutes.\n\n"
                    + "– Money Manager Team";
        } else {
            subject = "Verify your Money Manager account";
            body = "Hi " + user.getFullName() + ",\n\n"
                    + "Your 6-digit OTP verification code is:\n"
                    + rawOtp + "\n\n"
                    + "This OTP will expire in " + otpExpirationMinutes + " minutes.\n\n"
                    + "– Money Manager Team";
        }

        if ("phone".equalsIgnoreCase(deliveryType)) {
            if ("RESET".equalsIgnoreCase(purpose)) {
                smsService.sendSms(user.getPhoneNumber(), "Your Money Manager password reset OTP is " + rawOtp);
            } else {
                smsService.sendSms(user.getPhoneNumber(), "Your Money Manager verification OTP is " + rawOtp);
            }
        } else if ("both".equalsIgnoreCase(deliveryType)) {
            emailService.sendEmail(user.getEmail(), subject, body);
            if ("RESET".equalsIgnoreCase(purpose)) {
                smsService.sendSms(user.getPhoneNumber(), "Your Money Manager password reset OTP is " + rawOtp);
            } else {
                smsService.sendSms(user.getPhoneNumber(), "Your Money Manager verification OTP is " + rawOtp);
            }
        } else {
            emailService.sendEmail(user.getEmail(), subject, body);
        }
    }

    @Transactional
    public void verifyOtp(User user, String submittedCode) {
        String hashedCode = hashToken(submittedCode.trim());
        OtpVerification otp = otpRepo
                .findByUserAndOtpCodeAndIsUsedFalse(user, hashedCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP."));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired.");
        }

        // Mark used
        otp.setUsed(true);
        otpRepo.save(otp);
    }

    @Transactional
    public void generateAndSendOtp(User user, String deliveryType) {
        // Cooldown guard — check if there is an active (unused and unexpired) OTP
        java.util.Optional<OtpVerification> existing = otpRepo
                .findByUserAndIsUsedFalseAndExpiresAtAfter(user, LocalDateTime.now());

        if (existing.isPresent()) {
            log.warn("OTP already active for user {}. Skipping.", user.getEmail());
            return;
        }

        // Generate ONE OTP
        String rawOtp = String.format("%06d", new SecureRandom().nextInt(1000000));
        String hashedOtp = hashToken(rawOtp);

        // Save to DB
        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .otpCode(hashedOtp)
                .deliveryType(deliveryType.toUpperCase())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .build();
        otpRepo.save(otp);

        // Send to ONLY chosen channel
        String subject = "Verify your Money Manager account";
        String body = "Your OTP is: " + rawOtp + "\nExpires in 10 minutes.";

        if ("EMAIL".equalsIgnoreCase(deliveryType)) {
            log.info("Sending OTP via EMAIL to {}", user.getEmail());
            emailService.sendEmail(user.getEmail(), subject, body);
        } else if ("SMS".equalsIgnoreCase(deliveryType)) {
            log.info("Sending OTP via SMS to {}", user.getPhoneNumber());
            smsService.sendSms(user.getPhoneNumber(), "Your Money Manager OTP is " + rawOtp);
        }
    }

    private String hashToken(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
