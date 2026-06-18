package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.AdminUserDto;
import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.PaginatedUsersResponse;
import in.maheshshelakee.moneymanager.dto.UserStatusUpdateDTO;
import in.maheshshelakee.moneymanager.entity.Role;
import in.maheshshelakee.moneymanager.service.admin.AdminService;
import in.maheshshelakee.moneymanager.service.admin.AdminAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Unified admin user-management controller.
 * Restricts access to ADMIN.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;
    private final AdminAuditService adminAuditService;

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedUsersResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        PaginatedUsersResponse users = adminService.getAllUsersFiltered(page, size, search, role, status);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDto>> getUserById(@PathVariable Long id) {
        AdminUserDto user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDTO requestDTO,
            Authentication authentication,
            HttpServletRequest request) {

        String adminEmail = authentication.getName();
        String ipAddress = request.getRemoteAddr();

        AdminUserDto updated = adminService.updateUserStatus(
                id, requestDTO.getStatus(), requestDTO.getReason(), adminEmail, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(updated, "User status updated successfully"));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest req) {
        String roleStr = request.get("role");
        Role newRole = Role.valueOf(roleStr.toUpperCase());
        AdminUserDto updated = adminService.updateUserRole(id, newRole);

        String adminEmail = authentication.getName();
        String ipAddress = req.getRemoteAddr();
        adminAuditService.logAction(adminEmail, id, "UPDATE_USER_ROLE", "Changed role to " + newRole, ipAddress);

        return ResponseEntity.ok(ApiResponse.success(updated, "User role assigned successfully"));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<AdminUserDto>> toggleUserVerification(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest req) {
        AdminUserDto updated = adminService.toggleUserVerification(id);

        String adminEmail = authentication.getName();
        String ipAddress = req.getRemoteAddr();
        adminAuditService.logAction(adminEmail, id, "TOGGLE_USER_VERIFICATION",
                "Toggled verification badge status to: " + updated.getIsVerified(), ipAddress);

        return ResponseEntity.ok(ApiResponse.success(updated, "User verification badge status toggled"));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest req) {
        String newPassword = request.get("password");
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }
        adminService.resetUserPassword(id, newPassword);

        String adminEmail = authentication.getName();
        String ipAddress = req.getRemoteAddr();
        adminAuditService.logAction(adminEmail, id, "RESET_USER_PASSWORD", "Resetted user password by admin", ipAddress);

        return ResponseEntity.ok(ApiResponse.success(null, "User password reset successfully"));
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<AdminUserDto>> toggleUserActive(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest req) {
        AdminUserDto updated = adminService.toggleUserActive(id);

        String adminEmail = authentication.getName();
        String ipAddress = req.getRemoteAddr();
        adminAuditService.logAction(adminEmail, id, "TOGGLE_USER_ACTIVE",
                "Toggled active status to: " + updated.getIsActive(), ipAddress);

        return ResponseEntity.ok(ApiResponse.success(updated, "User active status toggled"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest req) {
        AdminUserDto targetUser = adminService.getUserById(id);
        adminService.deleteUser(id);

        String adminEmail = authentication.getName();
        String ipAddress = req.getRemoteAddr();
        adminAuditService.logAction(adminEmail, null, "DELETE_USER",
                String.format("Forcibly deleted user account: %s (ID: %d)", targetUser.getEmail(), id), ipAddress);

        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
