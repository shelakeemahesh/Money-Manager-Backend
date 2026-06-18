package in.maheshshelakee.moneymanager.service.admin;

import in.maheshshelakee.moneymanager.entity.PaymentHistoryEntity;
import in.maheshshelakee.moneymanager.entity.SubscriptionPlanEntity;
import in.maheshshelakee.moneymanager.entity.UserSubscriptionEntity;
import in.maheshshelakee.moneymanager.repository.PaymentHistoryRepository;
import in.maheshshelakee.moneymanager.repository.SubscriptionPlanRepository;
import in.maheshshelakee.moneymanager.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminSubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardMetrics() {
        List<UserSubscriptionEntity> allSubs = userSubscriptionRepository.findAll();
        
        long activeCount = 0;
        double mrr = 0.0;
        long churnThisMonth = 0;
        long upgradeCount = 2; // Simulated
        long downgradeCount = 1; // Simulated

        for (UserSubscriptionEntity sub : allSubs) {
            if ("ACTIVE".equals(sub.getStatus())) {
                activeCount++;
                if (sub.getPlan() != null && "MONTHLY".equals(sub.getPlan().getBillingCycle())) {
                    mrr += sub.getPlan().getPrice();
                } else if (sub.getPlan() != null && "YEARLY".equals(sub.getPlan().getBillingCycle())) {
                    mrr += sub.getPlan().getPrice() / 12.0;
                }
            } else if ("CANCELED".equals(sub.getStatus())) {
                churnThisMonth++; // Simplified churn count
            }
        }

        double churnRate = activeCount > 0 ? (double) churnThisMonth / (activeCount + churnThisMonth) * 100 : 0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeSubscribers", activeCount);
        metrics.put("mrr", mrr);
        metrics.put("churnRate", churnRate);
        metrics.put("upgradeCount", upgradeCount);
        metrics.put("downgradeCount", downgradeCount);

        return metrics;
    }

    @Transactional(readOnly = true)
    public Page<UserSubscriptionEntity> getAllSubscriptions(Pageable pageable) {
        return userSubscriptionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanEntity> getAllPlans() {
        return planRepository.findAll();
    }

    @Transactional
    public SubscriptionPlanEntity savePlan(SubscriptionPlanEntity plan) {
        return planRepository.save(plan);
    }

    @Transactional
    public UserSubscriptionEntity changePlanManually(Long userId, Long planId) {
        UserSubscriptionEntity sub = userSubscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user"));
        SubscriptionPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        sub.setRenewalDate(java.time.LocalDate.now().plusMonths(1));
        return userSubscriptionRepository.save(sub);
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryEntity> getPaymentHistory(Long userId) {
        return paymentHistoryRepository.findByUserIdOrderByPaymentDateDesc(userId);
    }
}
