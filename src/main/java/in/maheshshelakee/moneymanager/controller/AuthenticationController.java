package in.maheshshelakee.moneymanager.controller;

import in.maheshshelakee.moneymanager.dto.*;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.service.UserService;
import in.maheshshelakee.moneymanager.service.CloudinaryService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> registerProfile(@Valid @RequestBody UserDTO userDTO) {
        UserDTO registered = userService.registerUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(registered, "Registration successful. Please choose your OTP verification method."));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        userService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "OTP sent successfully!")));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        userService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Account verified successfully!")));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        userService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "OTP resent successfully!")));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshSession(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Refresh token is required"));
        }
        LoginResponse response = userService.refreshSession(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");
        if (refreshToken != null && !refreshToken.isBlank()) {
            userService.logout(refreshToken);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "If that email or phone number exists, an OTP has been sent.")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
            @Valid @RequestBody OtpResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Password has been reset successfully!")));
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Password changed successfully!")));
    }

    @GetMapping("/profile/upload-signature")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUploadSignature(
            @RequestParam(defaultValue = "moneymanager") String preset) {
        Map<String, Object> signatureData = cloudinaryService.getUploadSignature(preset);
        return ResponseEntity.ok(ApiResponse.success(signatureData));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(Principal principal) {
        User user = userService.getUserByEmail(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(userService.toDTO(user)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal) {
        UserDTO updated = userService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated successfully!"));
    }

    @PostMapping("/profile/wipe-data")
    public ResponseEntity<ApiResponse<Void>> wipeUserData(Principal principal) {
        userService.wipeUserData(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "User transactional data wiped successfully!"));
    }

    @PostMapping("/auth/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(@Valid @RequestBody GoogleOAuthRequest request) {
        LoginResponse response = userService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
