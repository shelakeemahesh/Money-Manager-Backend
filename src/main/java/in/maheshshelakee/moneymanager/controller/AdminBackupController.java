package in.maheshshelakee.moneymanager.controller;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.entity.BackupHistory;
import in.maheshshelakee.moneymanager.entity.BackupSettings;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.RetentionSettings;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import in.maheshshelakee.moneymanager.service.BackupRecoveryService;
import in.maheshshelakee.moneymanager.service.DataRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminBackupController {

    private final BackupRecoveryService backupRecoveryService;
    private final DataRetentionService dataRetentionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BackupHistory>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.success(backupRecoveryService.getBackupHistory()));
    }

    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<BackupHistory>> triggerBackup() {
        try {
            BackupHistory history = backupRecoveryService.triggerBackup("MANUAL");
            if ("FAILED".equals(history.getStatus())) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Backup process execution failed. Check server dump configurations."));
            }
            return ResponseEntity.ok(ApiResponse.success(history, "Backup completed successfully."));
        } catch (Exception e) {
            log.error("Manual database backup execution failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Backup failed: " + e.getMessage()));
        }
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<ApiResponse<Void>> restoreBackup(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String password = payload.get("password");
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Admin verification password is required."));
        }

        // Verify currently logged in SUPERADMIN password
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found."));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials. Restoration rejected."));
        }

        try {
            backupRecoveryService.restoreBackup(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Database restoration successful."));
        } catch (Exception e) {
            log.error("Database restoration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Restoration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/prepare-deploy")
    public ResponseEntity<ApiResponse<Void>> prepareDeploy(@RequestBody Map<String, String> payload) {
        String password = payload.get("password");
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Admin verification password is required."));
        }

        // Verify currently logged in admin password
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found."));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials. Deployment preparation rejected."));
        }

        try {
            backupRecoveryService.prepareForDeployment();
            return ResponseEntity.ok(ApiResponse.success(null, "Database cleaned successfully. Prepared for production deployment."));
        } catch (Exception e) {
            log.error("Deployment preparation database clearing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Database clear failed: " + e.getMessage()));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("backup", backupRecoveryService.getSettings());
        settings.put("retention", dataRetentionService.getSettings());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(@RequestBody Map<String, Object> payload) {
        // Parse backup settings
        Map<String, Object> backupMap = (Map<String, Object>) payload.get("backup");
        if (backupMap != null) {
            BackupSettings bs = BackupSettings.builder()
                    .frequency((String) backupMap.get("frequency"))
                    .retentionPeriodDays(Integer.parseInt(backupMap.get("retentionPeriodDays").toString()))
                    .storageDestination((String) backupMap.get("storageDestination"))
                    .awsBucket((String) backupMap.get("awsBucket"))
                    .awsAccessKey((String) backupMap.get("awsAccessKey"))
                    .awsSecretKey((String) backupMap.get("awsSecretKey"))
                    .awsRegion((String) backupMap.get("awsRegion"))
                    .build();
            backupRecoveryService.saveSettings(bs);
        }

        // Parse retention settings
        Map<String, Object> retentionMap = (Map<String, Object>) payload.get("retention");
        if (retentionMap != null) {
            RetentionSettings rs = RetentionSettings.builder()
                    .retentionPeriodDays(Integer.parseInt(retentionMap.get("retentionPeriodDays").toString()))
                    .build();
            dataRetentionService.saveSettings(rs);
        }

        return getSettings();
    }
}
