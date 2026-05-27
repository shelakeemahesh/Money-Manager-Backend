package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.IncomeEntity;
import in.maheshshelakee.moneymanager.entity.FraudEventEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import in.maheshshelakee.moneymanager.repository.FraudEventRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Slf4j
public class FraudDetectionService {

    private final FraudEventRepository fraudEventRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    public FraudDetectionService(
            @Lazy FraudEventRepository fraudEventRepository,
            @Lazy ExpenseRepository expenseRepository,
            @Lazy IncomeRepository incomeRepository,
            @Lazy UserRepository userRepository
    ) {
        this.fraudEventRepository = fraudEventRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.userRepository = userRepository;
    }

    public List<FraudEventEntity> getAllFlaggedEvents() {
        return fraudEventRepository.findAll();
    }

    public FraudEventEntity processFraudAction(Long eventId, String action) {
        FraudEventEntity event = fraudEventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Fraud event not found"));

        if (action.equalsIgnoreCase("CLEAR")) {
            event.setStatus("CLEARED");
            // Unflag the source transaction
            unflagTransaction(event.getTransactionId());
        } else if (action.equalsIgnoreCase("BLOCK")) {
            event.setStatus("BLOCKED");
            // Set threat score to maximum
            event.setRiskScore(100);
        }
        
        return fraudEventRepository.save(event);
    }

    private void unflagTransaction(Long txId) {
        Optional<ExpenseEntity> exp = expenseRepository.findById(txId);
        if (exp.isPresent()) {
            ExpenseEntity e = exp.get();
            e.setFlagged(false);
            expenseRepository.save(e);
            return;
        }
        Optional<IncomeEntity> inc = incomeRepository.findById(txId);
        if (inc.isPresent()) {
            IncomeEntity i = inc.get();
            i.setFlagged(false);
            incomeRepository.save(i);
        }
    }

    /**
     * Rules engine scanner. Runs every 15 minutes.
     * Evaluates recent transactions (e.g. created in last 15 minutes) for fraud indicators.
     */
    @Scheduled(fixedRate = 900000)
    public void runFraudRulesEngine() {
        log.info("Fraud Rules Engine: Starting scan on recent transaction entries...");
        LocalDateTime scanWindow = LocalDateTime.now().minusMinutes(15);

        List<ExpenseEntity> expenses = expenseRepository.findAll();
        for (ExpenseEntity exp : expenses) {
            if (exp.getCreatedAt() != null && exp.getCreatedAt().isAfter(scanWindow)) {
                // Evaluate duplicates
                checkDuplicateRule(exp);
                // Evaluate velocity
                checkVelocityRule(exp);
                // Evaluate unusual amount (z-score)
                checkZScoreRule(exp);
                // Evaluate off-hours
                checkOffHoursRule(exp);
            }
        }
    }

    private void checkDuplicateRule(ExpenseEntity exp) {
        LocalDateTime startWindow = exp.getCreatedAt().minusMinutes(5);
        LocalDateTime endWindow = exp.getCreatedAt().plusMinutes(5);
        
        List<ExpenseEntity> potentialDuplicates = expenseRepository.findAll().stream()
                .filter(e -> !e.getId().equals(exp.getId()) &&
                             e.getUser().getId().equals(exp.getUser().getId()) &&
                             e.getCategory().equalsIgnoreCase(exp.getCategory()) &&
                             e.getAmount().equals(exp.getAmount()) &&
                             e.getCreatedAt() != null &&
                             e.getCreatedAt().isAfter(startWindow) &&
                             e.getCreatedAt().isBefore(endWindow))
                .toList();

        if (!potentialDuplicates.isEmpty()) {
            flagTransaction(exp, "DUPLICATE", 60);
        }
    }

    private void checkVelocityRule(ExpenseEntity exp) {
        LocalDateTime startWindow = exp.getCreatedAt().minusHours(1);
        
        long count = expenseRepository.findAll().stream()
                .filter(e -> e.getUser().getId().equals(exp.getUser().getId()) &&
                             e.getCreatedAt() != null &&
                             e.getCreatedAt().isAfter(startWindow))
                .count();

        if (count > 10) {
            flagTransaction(exp, "VELOCITY_SPIKE", 80);
        }
    }

    private void checkZScoreRule(ExpenseEntity exp) {
        LocalDate startRange = LocalDate.now().minusDays(30);
        List<ExpenseEntity> history = expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDesc(
                exp.getUser(), startRange, LocalDate.now());

        if (history.size() < 5) return; // Need data density for standard deviation

        double mean = history.stream().mapToDouble(ExpenseEntity::getAmount).average().orElse(0.0);
        double varianceSum = history.stream()
                .mapToDouble(e -> Math.pow(e.getAmount() - mean, 2))
                .sum();
        double stdDev = Math.sqrt(varianceSum / history.size());

        if (stdDev > 0) {
            double zScore = (exp.getAmount() - mean) / stdDev;
            if (zScore > 2.5) {
                int risk = (int) Math.min(100, 50 + (zScore * 10));
                flagTransaction(exp, "UNUSUAL_AMOUNT", risk);
            }
        }
    }

    private void checkOffHoursRule(ExpenseEntity exp) {
        LocalTime time = exp.getCreatedAt().toLocalTime();
        if (time.isAfter(LocalTime.MIDNIGHT) && time.isBefore(LocalTime.of(4, 0))) {
            flagTransaction(exp, "OFF_HOURS", 30);
        }
    }

    private void flagTransaction(ExpenseEntity exp, String reason, int riskScore) {
        // Prevent duplicate flags for same transaction
        if (fraudEventRepository.findByTransactionId(exp.getId()).isPresent()) return;

        // Flag transaction in ledger
        exp.setFlagged(true);
        expenseRepository.save(exp);

        FraudEventEntity event = FraudEventEntity.builder()
                .transactionId(exp.getId())
                .userId(exp.getUser().getId())
                .userEmail(exp.getUser().getEmail())
                .amount(exp.getAmount())
                .flagReason(reason)
                .riskScore(riskScore)
                .status("UNDER_REVIEW")
                .build();

        fraudEventRepository.save(event);
        log.warn("Security Shield: Flagged transaction #{} for {} (Risk: {})", exp.getId(), reason, riskScore);
    }
}
