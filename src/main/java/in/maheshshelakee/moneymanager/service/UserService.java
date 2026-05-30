package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.dto.*;
import in.maheshshelakee.moneymanager.entity.CategoryEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.SessionEntity;
import in.maheshshelakee.moneymanager.entity.Role;
import in.maheshshelakee.moneymanager.entity.UserStatus;
import in.maheshshelakee.moneymanager.repository.CategoryRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import in.maheshshelakee.moneymanager.repository.SessionRepository;
import in.maheshshelakee.moneymanager.repository.OtpVerificationRepository;
import in.maheshshelakee.moneymanager.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionRepository sessionRepository;
    private final LoginAttemptService loginAttemptService;
    private final OtpAttemptService otpAttemptService;
    private final OtpVerificationRepository otpVerificationRepository;
    private final OtpService otpService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.otp-expiration-minutes:5}")
    private int otpExpirationMinutes;



    // ─── REGISTER ──────────────────────────────────────────────────────────────
    @Transactional
    public UserDTO registerUser(UserDTO userDTO) {
        String email = userDTO.getEmail().trim().toLowerCase();
        String phoneNumber = userDTO.getPhoneNumber().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
        }

        User newUser = User.builder()
                .fullName(userDTO.getFullName().trim())
                .email(email)
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .profileImage(userDTO.getProfileImage())
                .isActive(true)
                .isVerified(false)
                .status(UserStatus.ACTIVE)
                .role(Role.USER)
                .build();

        newUser = userRepository.save(newUser);

        // Create default categories
        createDefaultCategories(newUser);

        return toDTO(newUser);
    }

    // ─── VERIFY OTP ─────────────────────────────────────────────────────────────
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void verifyOtp(VerifyOtpRequest request) {
        String identifier = request.getEmailOrPhone().trim();

        if (otpAttemptService.isVerificationBlocked(identifier)) {
            int remaining = otpAttemptService.getRemainingVerificationCooldownMinutes(identifier);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed verification attempts. Please try again after " + remaining + " minutes.");
        }

        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is already verified");
        }

        try {
            otpService.verifyOtp(user, request.getOtpCode());
        } catch (ResponseStatusException ex) {
            otpAttemptService.verificationFailed(identifier);
            
            if (otpAttemptService.isVerificationBlocked(identifier)) {
                // Invalidate any active OTPs for this user
                otpVerificationRepository.deleteByUser(user);
                
                int remaining = otpAttemptService.getRemainingVerificationCooldownMinutes(identifier);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed verification attempts. Active OTP has been invalidated. Please try again after " + remaining + " minutes.");
            }
            throw ex;
        }

        user.setIsVerified(true);
        userRepository.save(user);

        otpAttemptService.verificationSucceeded(identifier);
    }

    // ─── RESEND OTP ─────────────────────────────────────────────────────────────
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String identifier = request.getEmailOrPhone().trim();
        String channel = request.getChannel(); // "email" or "phone"

        if (otpAttemptService.isRequestBlocked(identifier)) {
            int remaining = otpAttemptService.getRemainingRequestCooldownMinutes(identifier);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Please try again after " + remaining + " minutes.");
        }

        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is already verified");
        }

        otpService.generateAndSend(user, channel);

        otpAttemptService.recordRequest(identifier);
    }

    // ─── SEND OTP (NEW SINGLE CHANNEL FLOW) ─────────────────────────────────────
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        if (!"EMAIL".equalsIgnoreCase(request.getDeliveryType()) &&
                !"SMS".equalsIgnoreCase(request.getDeliveryType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliveryType must be EMAIL or SMS");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is already verified");
        }

        otpService.generateAndSendOtp(user, request.getDeliveryType());
    }

    // ─── LOGIN ─────────────────────────────────────────────────────────────────
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.getEmailOrPhone().trim();

        // Check Login lockouts
        if (loginAttemptService.isBlocked(identifier)) {
            int remainingCooldown = loginAttemptService.getRemainingCooldownMinutes(identifier);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again after " + remainingCooldown + " minutes.");
        }

        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> {
                    loginAttemptService.loginFailed(identifier);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(identifier);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Check verification & enabled state
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
        }

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not verified");
        }

        loginAttemptService.loginSucceeded(identifier);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate Access Token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                jwtUtil.getPasswordVersion(user.getPassword())
        );

        // Generate Refresh Token
        String refreshToken = UUID.randomUUID().toString();
        SessionEntity session = SessionEntity.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        sessionRepository.save(session);

        return new LoginResponse(token, refreshToken, toDTO(user));
    }

    // ─── REFRESH SESSION ────────────────────────────────────────────────────────
    @Transactional
    public LoginResponse refreshSession(String refreshToken) {
        SessionEntity session = sessionRepository.findByToken(refreshToken.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session");
        }

        User user = session.getUser();

        // Invalidate current refresh token and roll to a new one
        sessionRepository.delete(session);

        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                jwtUtil.getPasswordVersion(user.getPassword())
        );

        String newRefreshToken = UUID.randomUUID().toString();
        SessionEntity newSession = SessionEntity.builder()
                .user(user)
                .token(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        sessionRepository.save(newSession);

        return new LoginResponse(newAccessToken, newRefreshToken, toDTO(user));
    }

    // ─── LOGOUT ─────────────────────────────────────────────────────────────────
    @Transactional
    public void logout(String refreshToken) {
        sessionRepository.findByToken(refreshToken.trim())
                .ifPresent(sessionRepository::delete);
    }

    // ─── CHANGE PASSWORD ────────────────────────────────────────────────────────
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect current password");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete all active refresh tokens for the user
        sessionRepository.deleteByUserId(user.getId());
    }

    // ─── FORGOT PASSWORD ───────────────────────────────────────────────────────
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getEmail().trim().toLowerCase();

        if (otpAttemptService.isRequestBlocked(identifier)) {
            int remaining = otpAttemptService.getRemainingRequestCooldownMinutes(identifier);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Please try again after " + remaining + " minutes.");
        }

        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElse(null);

        if (user != null) {
            otpService.generateAndSend(user, "both", "RESET");
            otpAttemptService.recordRequest(identifier);
        }
    }

    // ─── RESET PASSWORD ────────────────────────────────────────────────────────
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void resetPassword(OtpResetPasswordRequest request) {
        String identifier = request.getEmailOrPhone().trim();

        if (otpAttemptService.isVerificationBlocked(identifier)) {
            int remaining = otpAttemptService.getRemainingVerificationCooldownMinutes(identifier);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed verification attempts. Please try again after " + remaining + " minutes.");
        }

        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        try {
            otpService.verifyOtp(user, request.getOtpCode());
        } catch (ResponseStatusException ex) {
            otpAttemptService.verificationFailed(identifier);
            
            if (otpAttemptService.isVerificationBlocked(identifier)) {
                // Invalidate any active OTPs for this user
                otpVerificationRepository.deleteByUser(user);
                
                int remaining = otpAttemptService.getRemainingVerificationCooldownMinutes(identifier);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed verification attempts. Active OTP has been invalidated. Please try again after " + remaining + " minutes.");
            }
            throw ex;
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete all active refresh tokens/sessions for the user
        sessionRepository.deleteByUserId(user.getId());

        otpAttemptService.verificationSucceeded(identifier);
    }

    // ─── DEFAULT CATEGORIES ─────────────────────────────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDefaultCategories(User user) {
        List<Object[]> defaults = List.of(
                new Object[]{"Salary","INCOME","💼","#22c55e"},
                new Object[]{"Freelance","INCOME","🖥️","#10b981"},
                new Object[]{"Investments","INCOME","📈","#06b6d4"},
                new Object[]{"Other Income","INCOME","💰","#6366f1"},
                new Object[]{"Food","EXPENSE","🍔","#ef4444"},
                new Object[]{"Transport","EXPENSE","🚗","#f97316"},
                new Object[]{"Shopping","EXPENSE","🛍️","#8b5cf6"},
                new Object[]{"Health","EXPENSE","🏥","#ec4899"},
                new Object[]{"Utilities","EXPENSE","💡","#f59e0b"},
                new Object[]{"Entertainment","EXPENSE","🎬","#14b8a6"},
                new Object[]{"Education","EXPENSE","📚","#3b82f6"},
                new Object[]{"Other","EXPENSE","📦","#6b7280"}
        );

        List<CategoryEntity> entities = defaults.stream()
                .map(row -> CategoryEntity.builder()
                        .name((String) row[0])
                        .type((String) row[1])
                        .icon((String) row[2])
                        .color((String) row[3])
                        .user(user)
                        .build())
                .toList();

        categoryRepository.saveAll(entities);
    }

    // ─── UPDATE PROFILE DETAILS ──────────────────────────────────────────────────
    @Transactional
    public UserDTO updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setFullName(request.getFullName().trim());
        if (request.getProfileImage() != null) {
            user.setProfileImage(request.getProfileImage().trim());
        }

        user = userRepository.save(user);
        return toDTO(user);
    }

    // ─── HELPER ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDTO toDTO(User entity) {
        return UserDTO.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .role(entity.getRole() != null ? entity.getRole().name() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .profileImage(entity.getProfileImage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
