package in.maheshshelakee.moneymanager.service.admin;

import com.zaxxer.hikari.HikariDataSource;
import in.maheshshelakee.moneymanager.entity.BackupHistory;
import in.maheshshelakee.moneymanager.entity.BackupSettings;
import in.maheshshelakee.moneymanager.repository.BackupHistoryRepository;
import in.maheshshelakee.moneymanager.repository.BackupSettingsRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupRecoveryService {

    private final BackupHistoryRepository historyRepository;
    private final BackupSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final DataSource dataSource;

    @Value("${app.backup.local-dir:backups}")
    private String localBackupDir;

    public BackupSettings getSettings() {
        return settingsRepository.findAll().stream().findFirst().orElse(null);
    }

    public BackupSettings saveSettings(BackupSettings newSettings) {
        BackupSettings current = getSettings();
        if (current != null) {
            newSettings.setId(current.getId());
        }
        return settingsRepository.save(newSettings);
    }

    public List<BackupHistory> getBackupHistory() {
        // Return sorted by timestamp desc
        return historyRepository.findAll().stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .toList();
    }

    public BackupHistory triggerBackup(String type) throws Exception {
        BackupSettings settings = getSettings();
        if (settings == null) {
            throw new IllegalStateException("Backup settings not initialized.");
        }

        if (!(dataSource instanceof HikariDataSource)) {
            throw new IllegalStateException("DataSource is not HikariCP. Cannot run automated backup.");
        }

        HikariDataSource hds = (HikariDataSource) dataSource;
        String jdbcUrl = hds.getJdbcUrl();
        String username = hds.getUsername();
        String password = hds.getPassword() != null ? hds.getPassword() : "";

        // Parse host, port, database name from JDBC url
        String dbType = jdbcUrl.contains("mysql") ? "mysql" : "postgresql";
        String cleanUrl = jdbcUrl.substring(jdbcUrl.indexOf("//") + 2);
        if (cleanUrl.contains("?")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.indexOf("?"));
        }
        String hostPort = cleanUrl.substring(0, cleanUrl.indexOf("/"));
        String dbName = cleanUrl.substring(cleanUrl.indexOf("/") + 1);

        String host = "localhost";
        String port = dbType.equals("mysql") ? "3306" : "5432";
        if (hostPort.contains(":")) {
            host = hostPort.substring(0, hostPort.indexOf(":"));
            port = hostPort.substring(hostPort.indexOf(":") + 1);
        } else {
            host = hostPort;
        }

        // Setup backup directory and file names
        File dir = new File(localBackupDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = dbType.equals("mysql") ? ".sql" : ".dump";
        String fileName = dbName + "_backup_" + timestamp + extension;
        File outputFile = new File(dir, fileName);

        ProcessBuilder pb;
        if (dbType.equals("mysql")) {
            // mysqldump commands
            pb = new ProcessBuilder(
                    "mysqldump",
                    "-h", host,
                    "-P", port,
                    "-u", username,
                    "-p" + password,
                    dbName
            );
            pb.redirectOutput(outputFile);
        } else {
            // pg_dump commands
            pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", host,
                    "-p", port,
                    "-U", username,
                    "-F", "c",
                    "-b",
                    "-v",
                    "-f", outputFile.getAbsolutePath(),
                    dbName
            );
            pb.environment().put("PGPASSWORD", password);
        }

        log.info("Starting db dump execution to file: {}", outputFile.getAbsolutePath());
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("Database dump command failed with exit code: {}", exitCode);
            // Save failure log
            BackupHistory history = BackupHistory.builder()
                    .timestamp(LocalDateTime.now())
                    .size(0L)
                    .type(type)
                    .status("FAILED")
                    .storageDestination(settings.getStorageDestination())
                    .fileName(fileName)
                    .filePathOrUrl("")
                    .build();
            return historyRepository.save(history);
        }

        log.info("Database dump successful. File size: {} bytes", outputFile.length());
        String filePathOrUrl = outputFile.getAbsolutePath();

        // Handle S3 Upload if selected
        if ("S3".equalsIgnoreCase(settings.getStorageDestination())) {
            try {
                uploadToS3(settings, outputFile, fileName);
                filePathOrUrl = "https://" + settings.getAwsBucket() + ".s3." + settings.getAwsRegion() + ".amazonaws.com/" + fileName;
                // Delete local file if backing up strictly to S3
                outputFile.delete();
            } catch (Exception e) {
                log.error("Failed to upload dump file to AWS S3. Falling back to local copy.", e);
            }
        }

        // Record history
        BackupHistory history = BackupHistory.builder()
                .timestamp(LocalDateTime.now())
                .size(outputFile.length())
                .type(type)
                .status("SUCCESS")
                .storageDestination(settings.getStorageDestination())
                .fileName(fileName)
                .filePathOrUrl(filePathOrUrl)
                .build();

        return historyRepository.save(history);
    }

    private void uploadToS3(BackupSettings settings, File file, String key) {
        Region region = Region.of(settings.getAwsRegion());
        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(settings.getAwsAccessKey(), settings.getAwsSecretKey())
                ))
                .build()) {

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(settings.getAwsBucket())
                            .key(key)
                            .build(),
                    Paths.get(file.getAbsolutePath())
            );
            log.info("Backup file successfully uploaded to S3 bucket: {}", settings.getAwsBucket());
        }
    }

    public void restoreBackup(Long backupId) throws Exception {
        BackupHistory history = historyRepository.findById(backupId)
                .orElseThrow(() -> new IllegalArgumentException("Backup record not found."));

        if (!"SUCCESS".equals(history.getStatus())) {
            throw new IllegalArgumentException("Cannot restore from a failed backup.");
        }

        BackupSettings settings = getSettings();
        if (settings == null) {
            throw new IllegalStateException("Backup settings not initialized.");
        }

        HikariDataSource hds = (HikariDataSource) dataSource;
        String jdbcUrl = hds.getJdbcUrl();
        String username = hds.getUsername();
        String password = hds.getPassword() != null ? hds.getPassword() : "";

        // Parse host, port, database name
        String dbType = jdbcUrl.contains("mysql") ? "mysql" : "postgresql";
        String cleanUrl = jdbcUrl.substring(jdbcUrl.indexOf("//") + 2);
        if (cleanUrl.contains("?")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.indexOf("?"));
        }
        String hostPort = cleanUrl.substring(0, cleanUrl.indexOf("/"));
        String dbName = cleanUrl.substring(cleanUrl.indexOf("/") + 1);

        String host = "localhost";
        String port = dbType.equals("mysql") ? "3306" : "5432";
        if (hostPort.contains(":")) {
            host = hostPort.substring(0, hostPort.indexOf(":"));
            port = hostPort.substring(hostPort.indexOf(":") + 1);
        } else {
            host = hostPort;
        }

        File targetFile;

        // If file is on S3, download it to local backups directory first
        if ("S3".equalsIgnoreCase(history.getStorageDestination())) {
            targetFile = new File(localBackupDir, "temp_restore_" + history.getFileName());
            log.info("Downloading backup file from S3 for restoration...");
            downloadFromS3(settings, history.getFileName(), targetFile);
        } else {
            targetFile = new File(history.getFilePathOrUrl());
        }

        if (!targetFile.exists()) {
            throw new IllegalArgumentException("Backup file does not exist locally or could not be downloaded.");
        }

        ProcessBuilder pb;
        if (dbType.equals("mysql")) {
            // mysql restore commands
            pb = new ProcessBuilder(
                    "mysql",
                    "-h", host,
                    "-P", port,
                    "-u", username,
                    "-p" + password,
                    dbName
            );
            pb.redirectInput(targetFile);
        } else {
            // pg_restore commands
            pb = new ProcessBuilder(
                    "pg_restore",
                    "-h", host,
                    "-p", port,
                    "-U", username,
                    "-d", dbName,
                    "-c", // Clean/drop database objects before recreating
                    targetFile.getAbsolutePath()
            );
            pb.environment().put("PGPASSWORD", password);
        }

        log.info("Starting restoration process from file: {}", targetFile.getAbsolutePath());
        Process process = pb.start();
        int exitCode = process.waitFor();

        // Cleanup temp file if downloaded from S3
        if ("S3".equalsIgnoreCase(history.getStorageDestination())) {
            targetFile.delete();
        }

        if (exitCode != 0) {
            log.error("Database restore command failed with exit code: {}", exitCode);
            throw new RuntimeException("Database restoration failed. Verify system dump logs.");
        }

        log.info("Database restoration complete. Restarting application context might be recommended.");
    }

    private void downloadFromS3(BackupSettings settings, String key, File destination) throws Exception {
        Region region = Region.of(settings.getAwsRegion());
        try (S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(settings.getAwsAccessKey(), settings.getAwsSecretKey())
                ))
                .build()) {

            Files.copy(
                    s3.getObject(GetObjectRequest.builder().bucket(settings.getAwsBucket()).key(key).build()),
                    Paths.get(destination.getAbsolutePath())
            );
        }
    }

    // Cron expression scheduled backup check (Runs daily at 2:00 AM)
    @Scheduled(cron = "0 0 2 * * ?")
    public void runScheduledBackup() {
        BackupSettings settings = getSettings();
        if (settings == null) return;

        boolean shouldRun = false;
        if ("DAILY".equalsIgnoreCase(settings.getFrequency())) {
            shouldRun = true;
        } else if ("WEEKLY".equalsIgnoreCase(settings.getFrequency())) {
            shouldRun = java.time.LocalDate.now().getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
        }

        if (shouldRun) {
            log.info("Triggering scheduled database backup...");
            try {
                triggerBackup("AUTOMATED");
            } catch (Exception e) {
                log.error("Scheduled database backup process failed", e);
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void prepareForDeployment() throws Exception {
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            String dbProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            
            if (dbProductName.contains("mysql")) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                stmt.execute("TRUNCATE TABLE tbl_expenses");
                stmt.execute("TRUNCATE TABLE tbl_incomes");
                stmt.execute("TRUNCATE TABLE tbl_budgets");
                stmt.execute("TRUNCATE TABLE tbl_subcategories");
                stmt.execute("TRUNCATE TABLE tbl_categories");
                stmt.execute("TRUNCATE TABLE fraud_events");
                stmt.execute("TRUNCATE TABLE tbl_notifications");
                stmt.execute("TRUNCATE TABLE tbl_support_tickets");
                stmt.execute("TRUNCATE TABLE tbl_payment_history");
                stmt.execute("TRUNCATE TABLE tbl_ticket_replies");
                stmt.execute("TRUNCATE TABLE tbl_user_feedbacks");
                stmt.execute("TRUNCATE TABLE tbl_user_subscriptions");
                stmt.execute("TRUNCATE TABLE tbl_subscriptions");
                stmt.execute("TRUNCATE TABLE tbl_system_error_logs");
                stmt.execute("TRUNCATE TABLE tbl_ai_logs");
                stmt.execute("TRUNCATE TABLE tbl_admin_audit_logs");
                stmt.execute("TRUNCATE TABLE tbl_backup_history");
                stmt.execute("TRUNCATE TABLE tbl_password_resets");
                stmt.execute("TRUNCATE TABLE tbl_sessions");
                stmt.execute("TRUNCATE TABLE tbl_reports");
                stmt.execute("DELETE FROM tbl_users WHERE email != 'shelakemahesh024@gmail.com'");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            } else {
                stmt.execute("TRUNCATE TABLE tbl_expenses, tbl_incomes, tbl_budgets, tbl_subcategories, tbl_categories, fraud_events, tbl_notifications, tbl_support_tickets, tbl_payment_history, tbl_ticket_replies, tbl_user_feedbacks, tbl_user_subscriptions, tbl_subscriptions, tbl_system_error_logs, tbl_ai_logs, tbl_admin_audit_logs, tbl_backup_history, tbl_password_resets, tbl_sessions, tbl_reports CASCADE");
                stmt.execute("DELETE FROM tbl_users WHERE email != 'shelakemahesh024@gmail.com'");
            }
            log.info("Database cleaned successfully. Prepared for deployment. Only admin user left.");
        }
    }
}
