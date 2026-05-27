package in.maheshshelakee.moneymanager.config;

import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.Role;
import in.maheshshelakee.moneymanager.entity.UserStatus;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import in.maheshshelakee.moneymanager.repository.AISettingsRepository;
import in.maheshshelakee.moneymanager.repository.BudgetRepository;
import in.maheshshelakee.moneymanager.repository.SubscriptionPlanRepository;
import in.maheshshelakee.moneymanager.repository.UserSubscriptionRepository;
import in.maheshshelakee.moneymanager.repository.NotificationTemplateRepository;
import in.maheshshelakee.moneymanager.entity.AISettingsEntity;
import in.maheshshelakee.moneymanager.entity.BudgetEntity;
import in.maheshshelakee.moneymanager.entity.SubscriptionPlanEntity;
import in.maheshshelakee.moneymanager.entity.UserSubscriptionEntity;
import in.maheshshelakee.moneymanager.entity.NotificationTemplateEntity;
import in.maheshshelakee.moneymanager.entity.SupportTicketEntity;
import in.maheshshelakee.moneymanager.entity.TicketReplyEntity;
import in.maheshshelakee.moneymanager.entity.UserFeedbackEntity;
import in.maheshshelakee.moneymanager.entity.FraudEventEntity;
import in.maheshshelakee.moneymanager.entity.BackupSettings;
import in.maheshshelakee.moneymanager.entity.RetentionSettings;
import in.maheshshelakee.moneymanager.repository.SupportTicketRepository;
import in.maheshshelakee.moneymanager.repository.TicketReplyRepository;
import in.maheshshelakee.moneymanager.repository.UserFeedbackRepository;
import in.maheshshelakee.moneymanager.repository.FraudEventRepository;
import in.maheshshelakee.moneymanager.repository.BackupSettingsRepository;
import in.maheshshelakee.moneymanager.repository.RetentionSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final AISettingsRepository aiSettingsRepository;
    private final BudgetRepository budgetRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final NotificationTemplateRepository templateRepository;
    private final SupportTicketRepository ticketRepository;
    private final TicketReplyRepository replyRepository;
    private final UserFeedbackRepository feedbackRepository;
    private final FraudEventRepository fraudEventRepository;
    private final BackupSettingsRepository backupSettingsRepository;
    private final RetentionSettingsRepository retentionSettingsRepository;

    @Override
    public void run(String... args) throws Exception {
        // Self-healing database column alter to support new enum values and clean legacy keys
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String dbProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (dbProductName.contains("mysql")) {
                stmt.execute("ALTER TABLE tbl_users MODIFY COLUMN role VARCHAR(50)");
                stmt.execute("ALTER TABLE tbl_users MODIFY COLUMN status VARCHAR(50)");
            } else if (dbProductName.contains("postgresql")) {
                stmt.execute("ALTER TABLE tbl_users ALTER COLUMN role TYPE VARCHAR(50)");
                stmt.execute("ALTER TABLE tbl_users ALTER COLUMN status TYPE VARCHAR(50)");
                
                // Drop check constraints to allow new roles/status values
                try {
                    stmt.execute("ALTER TABLE tbl_users DROP CONSTRAINT IF EXISTS tbl_users_role_check");
                } catch (Exception ex) {
                    log.warn("Drop role check constraint failed: " + ex.getMessage());
                }
                try {
                    stmt.execute("ALTER TABLE tbl_users DROP CONSTRAINT IF EXISTS tbl_users_status_check");
                } catch (Exception ex) {
                    log.warn("Drop status check constraint failed: " + ex.getMessage());
                }

                // Drop legacy foreign keys pointing to dropped tbl_profiles table
                try {
                    stmt.execute("ALTER TABLE tbl_admin_audit_logs DROP CONSTRAINT IF EXISTS fk70jgn3boq478rrw2lk0blh7ai");
                } catch (Exception ex) {
                    log.warn("Drop legacy target_user_id FK constraint failed: " + ex.getMessage());
                }
                try {
                    stmt.execute("ALTER TABLE tbl_admin_audit_logs DROP CONSTRAINT IF EXISTS fknd1sypx5h959k1v9u1hsgvx4u");
                } catch (Exception ex) {
                    log.warn("Drop legacy admin_id FK constraint failed: " + ex.getMessage());
                }
            }
            stmt.execute("UPDATE tbl_users SET role = 'ADMIN' WHERE role = 'SUPERADMIN'");
            log.info("Successfully relaxed tbl_users column constraints, dropped legacy FKs, and migrated SUPERADMIN to ADMIN.");
        } catch (Exception e) {
            log.warn("Database alter/migration log (non-fatal): " + e.getMessage());
        }

        // Seed regular Admin if none exists
        String adminEmail = "shelakemahesh024@gmail.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .fullName("Admin")
                    .email(adminEmail)
                    .phoneNumber("+919876543210")
                    .password(passwordEncoder.encode("Mahesh@3459"))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded admin: email={}, password=Mahesh@3459", adminEmail);
        }

        // Seed Default AI Settings
        if (aiSettingsRepository.count() == 0) {
            AISettingsEntity aiSettings = AISettingsEntity.builder()
                    .globalAiEnabled(true)
                    .confidenceThreshold(0.75)
                    .predictionWindowDays(14)
                    .build();
            aiSettingsRepository.save(aiSettings);
            log.info("Seeded default AI settings.");
        }

        // Seed Subscription Plans
        if (planRepository.count() == 0) {
            planRepository.save(SubscriptionPlanEntity.builder().name("Free").price(0.0).billingCycle("MONTHLY").features("[\"Basic Tracking\"]").isActive(true).build());
            planRepository.save(SubscriptionPlanEntity.builder().name("Pro").price(150.0).billingCycle("MONTHLY").features("[\"AI Insights\", \"Unlimited Tracking\"]").isActive(true).build());
            planRepository.save(SubscriptionPlanEntity.builder().name("Enterprise").price(1199.0).billingCycle("YEARLY").features("[\"Custom Budgets\", \"API Access\"]").isActive(true).build());
            log.info("Seeded subscription plans.");
        }

        // Seed Notification Templates
        if (templateRepository.count() == 0) {
            templateRepository.save(NotificationTemplateEntity.builder().eventName("BUDGET_EXCEEDED").subjectTemplate("Budget Exceeded: {{category}}").bodyTemplate("You have exceeded your budget for {{category}} by {{amount}}.").build());
            templateRepository.save(NotificationTemplateEntity.builder().eventName("SUSPICIOUS_TRANSACTION").subjectTemplate("Suspicious Activity Detected").bodyTemplate("We detected a suspicious transaction of {{amount}}.").build());
            templateRepository.save(NotificationTemplateEntity.builder().eventName("SUBSCRIPTION_RENEWAL").subjectTemplate("Subscription Renewal").bodyTemplate("Your {{plan}} subscription has been renewed successfully.").build());
            log.info("Seeded default notification templates.");
        }

        // Seed Backup Settings
        if (backupSettingsRepository.count() == 0) {
            BackupSettings settings = BackupSettings.builder()
                    .frequency("DAILY")
                    .retentionPeriodDays(30)
                    .storageDestination("LOCAL")
                    .build();
            backupSettingsRepository.save(settings);
            log.info("Seeded default backup settings.");
        }

        // Seed Retention Settings
        if (retentionSettingsRepository.count() == 0) {
            RetentionSettings settings = RetentionSettings.builder()
                    .retentionPeriodDays(30)
                    .build();
            retentionSettingsRepository.save(settings);
            log.info("Seeded default data retention settings.");
        }
    }
}
