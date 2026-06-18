package in.maheshshelakee.moneymanager.service.user;

import in.maheshshelakee.moneymanager.dto.SubscriptionRequestDTO;
import in.maheshshelakee.moneymanager.dto.SubscriptionStatusDTO;
import in.maheshshelakee.moneymanager.entity.Role;
import in.maheshshelakee.moneymanager.entity.SubscriptionEntity;
import in.maheshshelakee.moneymanager.entity.SubscriptionPlanEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.UserSubscriptionEntity;
import in.maheshshelakee.moneymanager.repository.SubscriptionPlanRepository;
import in.maheshshelakee.moneymanager.repository.SubscriptionRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import in.maheshshelakee.moneymanager.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Transactional
    public SubscriptionEntity submitUpgrade(User user, SubscriptionRequestDTO request) {
        String planType = request.getPlanType().toUpperCase();
        if (!"MONTHLY".equals(planType) && !"YEARLY".equals(planType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid plan type. Must be MONTHLY or YEARLY.");
        }

        // Check if there is already a pending verification request for this user
        if (subscriptionRepository.existsByUserIdAndPaymentStatus(user.getId(), "PENDING")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "You already have a pending upgrade request. Please wait for verification.");
        }

        // Check duplicate transaction ID / UTR
        String utr = request.getTransactionId().trim();
        if (subscriptionRepository.findByTransactionId(utr).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "This Transaction ID / UTR has already been submitted.");
        }

        double amount = "MONTHLY".equals(planType) ? 99.0 : 799.0;

        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .user(user)
                .planType(planType)
                .amount(amount)
                .paymentMethod("UPI")
                .transactionId(utr)
                .paymentStatus("PENDING")
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusDTO getMySubscriptionStatus(User user) {
        List<SubscriptionEntity> subs = subscriptionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId());
        if (subs.isEmpty()) {
            return SubscriptionStatusDTO.builder()
                    .status("NONE")
                    .build();
        }

        SubscriptionEntity latest = subs.get(0);
        String status = latest.getPaymentStatus();
        Long remainingDays = 0L;

        if ("APPROVED".equals(status) && latest.getExpiryDate() != null) {
            if (latest.getExpiryDate().isBefore(LocalDateTime.now())) {
                status = "EXPIRED";
            } else {
                remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), latest.getExpiryDate());
            }
        }

        return SubscriptionStatusDTO.builder()
                .status(status)
                .planType(latest.getPlanType())
                .amount(latest.getAmount())
                .transactionId(latest.getTransactionId())
                .submittedAt(latest.getSubmittedAt())
                .activatedAt(latest.getActivatedAt())
                .expiryDate(latest.getExpiryDate())
                .remainingDays(remainingDays)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionEntity> getPendingRequests() {
        return subscriptionRepository.findByPaymentStatusOrderBySubmittedAtDesc("PENDING");
    }

    @Transactional
    public SubscriptionEntity approveSubscription(Long id) {
        SubscriptionEntity subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription request not found"));

        if (!"PENDING".equals(subscription.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription request is not in PENDING status");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = "MONTHLY".equals(subscription.getPlanType()) ? now.plusMonths(1) : now.plusYears(1);

        subscription.setPaymentStatus("APPROVED");
        subscription.setActivatedAt(now);
        subscription.setExpiryDate(expiry);
        final SubscriptionEntity savedSub = subscriptionRepository.save(subscription);

        // Update User Role to PRO
        User user = savedSub.getUser();
        user.setRole(Role.PRO);
        userRepository.save(user);

        // Find Pro plan to link
        SubscriptionPlanEntity proPlan = planRepository.findAll().stream()
                .filter(p -> "Pro".equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElseGet(() -> planRepository.save(
                        SubscriptionPlanEntity.builder()
                                .name("Pro")
                                .price("MONTHLY".equals(savedSub.getPlanType()) ? 99.0 : 799.0)
                                .billingCycle(savedSub.getPlanType())
                                .features("[\"AI Insights\", \"Unlimited Transactions\"]")
                                .isActive(true)
                                .build()
                ));

        // Create or update UserSubscriptionEntity
        UserSubscriptionEntity userSub = userSubscriptionRepository.findByUserId(user.getId())
                .orElse(UserSubscriptionEntity.builder()
                        .user(user)
                        .build());

        userSub.setPlan(proPlan);
        userSub.setStatus("ACTIVE");
        userSub.setStartDate(LocalDate.now());
        userSub.setRenewalDate(expiry.toLocalDate());
        userSubscriptionRepository.save(userSub);

        return subscription;
    }

    @Transactional
    public SubscriptionEntity rejectSubscription(Long id) {
        SubscriptionEntity subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription request not found"));

        if (!"PENDING".equals(subscription.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription request is not in PENDING status");
        }

        subscription.setPaymentStatus("REJECTED");
        return subscriptionRepository.save(subscription);
    }
}
