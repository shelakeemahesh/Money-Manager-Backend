package in.maheshshelakee.moneymanager.service.admin;

import in.maheshshelakee.moneymanager.entity.*;
import in.maheshshelakee.moneymanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {

    private final RetentionSettingsRepository retentionSettingsRepository;
    private final UserRepository userRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SupportTicketRepository ticketRepository;
    private final TicketReplyRepository replyRepository;
    private final UserFeedbackRepository feedbackRepository;

    public RetentionSettings getSettings() {
        return retentionSettingsRepository.findAll().stream().findFirst().orElse(null);
    }

    public RetentionSettings saveSettings(RetentionSettings newSettings) {
        RetentionSettings current = getSettings();
        if (current != null) {
            newSettings.setId(current.getId());
        }
        return retentionSettingsRepository.save(newSettings);
    }

    // Scheduled retention check (Runs daily at 1:00 AM)
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void runDataRetentionPurge() {
        RetentionSettings settings = getSettings();
        if (settings == null) {
            log.warn("Retention settings not initialized. Skipping purge.");
            return;
        }

        int retentionDays = settings.getRetentionPeriodDays();
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(retentionDays);

        log.info("Starting data retention purge. Threshold date: {}", thresholdDate);

        // Find profiles that are not ACTIVE and whose updatedAt is older than thresholdDate
        List<User> profilesToPurge = userRepository.findAll().stream()
                .filter(p -> p.getStatus() != UserStatus.ACTIVE)
                .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isBefore(thresholdDate))
                .toList();

        log.info("Found {} profiles to purge based on retention policy ({} days).", profilesToPurge.size(), retentionDays);

        for (User user : profilesToPurge) {
            try {
                purgeUserProfileData(user);
                log.info("Successfully purged profile and related data for: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to purge profile data for user: " + user.getEmail(), e);
            }
        }
    }

    @Transactional
    public void purgeUserProfileData(User user) {
        // 1. Delete Incomes
        List<IncomeEntity> incomes = incomeRepository.findByUserOrderByDateDesc(user);
        incomeRepository.deleteAll(incomes);

        // 2. Delete Expenses
        List<ExpenseEntity> expenses = expenseRepository.findByUserOrderByExpenseDateDesc(user);
        expenseRepository.deleteAll(expenses);

        // 3. Delete Budgets
        List<BudgetEntity> budgets = budgetRepository.findByUserId(user.getId());
        budgetRepository.deleteAll(budgets);

        // 4. Delete Categories
        List<CategoryEntity> categories = categoryRepository.findByUserWithSubcategories(user);
        categoryRepository.deleteAll(categories);

        // 5. Delete Subscriptions
        Optional<UserSubscriptionEntity> sub = userSubscriptionRepository.findByUserId(user.getId());
        sub.ifPresent(userSubscriptionRepository::delete);

        // 6. Delete Support Ticket Replies (sent by user)
        List<TicketReplyEntity> sentReplies = replyRepository.findAll().stream()
                .filter(r -> r.getSender() != null && r.getSender().getId().equals(user.getId()))
                .toList();
        replyRepository.deleteAll(sentReplies);

        // 7. Delete Support Tickets (and their replies)
        List<SupportTicketEntity> userTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getUser() != null && t.getUser().getId().equals(user.getId()))
                .toList();
        for (SupportTicketEntity ticket : userTickets) {
            List<TicketReplyEntity> ticketReplies = replyRepository.findAll().stream()
                    .filter(r -> r.getTicket() != null && r.getTicket().getId().equals(ticket.getId()))
                    .toList();
            replyRepository.deleteAll(ticketReplies);
            ticketRepository.delete(ticket);
        }

        // 8. Delete Feedback
        List<UserFeedbackEntity> feedbackList = feedbackRepository.findAll().stream()
                .filter(f -> f.getUser() != null && f.getUser().getId().equals(user.getId()))
                .toList();
        feedbackRepository.deleteAll(feedbackList);

        // 9. Delete Profile Entity
        userRepository.delete(user);
    }
}
