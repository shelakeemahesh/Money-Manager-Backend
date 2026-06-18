package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.entity.PaymentHistoryEntity;
import in.maheshshelakee.moneymanager.entity.SubscriptionEntity;
import in.maheshshelakee.moneymanager.entity.SubscriptionPlanEntity;
import in.maheshshelakee.moneymanager.entity.UserSubscriptionEntity;
import in.maheshshelakee.moneymanager.service.admin.AdminSubscriptionService;
import in.maheshshelakee.moneymanager.service.user.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubscriptionController {

    private final AdminSubscriptionService subscriptionService;
    private final SubscriptionService manualSubscriptionService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getDashboardMetrics()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserSubscriptionEntity>>> getSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAllSubscriptions(PageRequest.of(page, size))));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanEntity>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAllPlans()));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<SubscriptionPlanEntity>> savePlan(@RequestBody SubscriptionPlanEntity plan) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.savePlan(plan)));
    }

    @PostMapping("/change-plan")
    public ResponseEntity<ApiResponse<UserSubscriptionEntity>> changePlan(
            @RequestParam Long userId, @RequestParam Long planId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.changePlanManually(userId, planId)));
    }

    @GetMapping("/payments/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentHistoryEntity>>> getPayments(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getPaymentHistory(userId)));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<SubscriptionEntity>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.success(manualSubscriptionService.getPendingRequests()));
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<ApiResponse<SubscriptionEntity>> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manualSubscriptionService.approveSubscription(id), "Subscription approved and activated."));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<ApiResponse<SubscriptionEntity>> rejectRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manualSubscriptionService.rejectSubscription(id), "Subscription request rejected."));
    }
}
