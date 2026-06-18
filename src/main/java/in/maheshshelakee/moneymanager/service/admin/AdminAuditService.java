package in.maheshshelakee.moneymanager.service.admin;

import in.maheshshelakee.moneymanager.dto.AdminAuditLogDto;
import in.maheshshelakee.moneymanager.entity.AdminAuditLog;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.AdminAuditLogRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final UserRepository userRepository;

    /**
     * Returns paginated audit logs, converted to DTOs to avoid
     * exposing JPA entities (and their lazy associations) to the client.
     */
    public Page<AdminAuditLogDto> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return adminAuditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    // FIX 1: Removed manual .createdAt(LocalDateTime.now()) from the builder.
    //         @CreationTimestamp on AdminAuditLog.createdAt handles timestamping automatically.
    //         Setting it manually via Lombok builder was redundant and bypassed the annotation.
    //
    // FIX 2: @Async now works because @EnableAsync was added to MoneymanagerApplication.
    //         Added Slf4j so uncaught async exceptions are logged rather than silently swallowed.
    @Async
    public void logAction(String adminEmail, Long targetUserId, String action, String details, String ipAddress) {
        try {
            User admin = userRepository.findByEmail(adminEmail).orElse(null);
            User targetUser = targetUserId != null
                    ? userRepository.findById(targetUserId).orElse(null)
                    : null;

            if (admin == null) {
                log.warn("AdminAuditService: admin not found for email '{}', skipping audit log", adminEmail);
                return;
            }

            AdminAuditLog logEntry = AdminAuditLog.builder()
                    .admin(admin)
                    .targetUser(targetUser)
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    // FIX: createdAt intentionally omitted — @CreationTimestamp sets it on persist
                    .build();

            adminAuditLogRepository.save(logEntry);
        } catch (Exception ex) {
            // Async exceptions are otherwise silently swallowed
            log.error("AdminAuditService: failed to save audit log for admin='{}' action='{}'",
                    adminEmail, action, ex);
        }
    }

    private AdminAuditLogDto toDto(AdminAuditLog entity) {
        return AdminAuditLogDto.builder()
                .id(entity.getId())
                .adminEmail(entity.getAdmin() != null ? entity.getAdmin().getEmail() : null)
                .targetUserEmail(entity.getTargetUser() != null ? entity.getTargetUser().getEmail() : null)
                .action(entity.getAction())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

