package in.maheshshelakee.moneymanager.controller;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.SubscriptionRequestDTO;
import in.maheshshelakee.moneymanager.dto.SubscriptionStatusDTO;
import in.maheshshelakee.moneymanager.entity.SubscriptionEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.service.SubscriptionService;
import in.maheshshelakee.moneymanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse<SubscriptionEntity>> submitUpgrade(
            @Valid @RequestBody SubscriptionRequestDTO request,
            Principal principal) {
        User user = userService.getUserByEmail(principal.getName());
        SubscriptionEntity response = subscriptionService.submitUpgrade(user, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment submitted successfully. Your Professional account will be activated within 30 minutes after verification."));
    }

    @GetMapping("/my-status")
    public ResponseEntity<ApiResponse<SubscriptionStatusDTO>> getMyStatus(Principal principal) {
        User user = userService.getUserByEmail(principal.getName());
        SubscriptionStatusDTO status = subscriptionService.getMySubscriptionStatus(user);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
