package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.dto.AdminDashboardResponse;
import in.maheshshelakee.moneymanager.dto.AdminStatsResponse;
import in.maheshshelakee.moneymanager.dto.AdminUserDto;
import in.maheshshelakee.moneymanager.entity.AdminAuditLog;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.UserStatus;
import in.maheshshelakee.moneymanager.exception.ResourceNotFoundException;
import in.maheshshelakee.moneymanager.repository.AdminAuditLogRepository;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        double totalIncome = incomeRepository.sumAllIncomes();
        double totalExpense = expenseRepository.sumAllExpenses();

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .build();
    }

    public Page<AdminUserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toDto);
    }

    public in.maheshshelakee.moneymanager.dto.PaginatedUsersResponse getAllUsersFiltered(
            int page, int size, String search, String roleStr, String statusStr) {
        in.maheshshelakee.moneymanager.entity.Role role = null;
        if (roleStr != null && !roleStr.isBlank() && !"ALL".equalsIgnoreCase(roleStr)) {
            role = in.maheshshelakee.moneymanager.entity.Role.valueOf(roleStr.toUpperCase());
        }
        UserStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            status = UserStatus.valueOf(statusStr.toUpperCase());
        }
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<User> pageResult = userRepository.findAllFiltered(cleanSearch, role, status, pageable);

        java.util.List<AdminUserDto> dtoList = pageResult.getContent().stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());

        return in.maheshshelakee.moneymanager.dto.PaginatedUsersResponse.builder()
                .users(dtoList)
                .totalCount(pageResult.getTotalElements())
                .page(page)
                .pageSize(size)
                .build();
    }

    @Transactional
    public AdminUserDto updateUserRole(Long userId, in.maheshshelakee.moneymanager.entity.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(newRole);
        userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public AdminUserDto toggleUserVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsVerified(user.getIsVerified() == null ? true : !user.getIsVerified());
        userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
    }

    public AdminUserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toDto(user);
    }

    /**
     * Returns detailed user statistics (total, active, suspended, banned).
     * Moved from AdminStatsController to proper service layer.
     */
    public AdminStatsResponse getSystemStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .suspendedUsers(userRepository.countByStatus(UserStatus.SUSPENDED))
                .bannedUsers(userRepository.countByStatus(UserStatus.BANNED))
                .build();
    }

    @Transactional
    public AdminUserDto updateUserStatus(Long userId, UserStatus newStatus, String reason,
                                         String adminEmail, String ipAddress) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserStatus oldStatus = targetUser.getStatus();
        targetUser.setStatus(newStatus);

        if (newStatus == UserStatus.BANNED || newStatus == UserStatus.SUSPENDED) {
            targetUser.setIsActive(false);
        } else if (newStatus == UserStatus.ACTIVE && oldStatus != UserStatus.ACTIVE) {
            targetUser.setIsActive(true);
        }

        userRepository.save(targetUser);

        AdminAuditLog logEntry = AdminAuditLog.builder()
                .admin(admin)
                .targetUser(targetUser)
                .action("UPDATE_USER_STATUS")
                .details(String.format("Changed status from %s to %s for user %s. Reason: %s",
                        oldStatus, newStatus, targetUser.getEmail(), reason))
                .ipAddress(ipAddress)
                .build();

        adminAuditLogRepository.save(logEntry);

        log.info("Admin {} changed status of user {} from {} to {}",
                adminEmail, targetUser.getEmail(), oldStatus, newStatus);
 
        return toDto(targetUser);
    }
 
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
 
    @Transactional
    public AdminUserDto toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(user.getIsActive() == null ? true : !user.getIsActive());
        userRepository.save(user);
        return toDto(user);
    }
 
    private AdminUserDto toDto(User entity) {
        return AdminUserDto.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .createdAt(entity.getCreatedAt())
                .isActive(entity.getIsActive())
                .status(entity.getStatus())
                .role(entity.getRole())
                .isVerified(entity.getIsVerified())
                .build();
    }
}
