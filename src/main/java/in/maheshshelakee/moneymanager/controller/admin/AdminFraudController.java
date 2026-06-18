package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.entity.FraudEventEntity;
import in.maheshshelakee.moneymanager.service.admin.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/fraud")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFraudController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<FraudEventEntity>>> getFlaggedEvents() {
        List<FraudEventEntity> events = fraudDetectionService.getAllFlaggedEvents();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping("/events/{id}/action")
    public ResponseEntity<ApiResponse<FraudEventEntity>> handleFraudAction(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        
        String action = payload.get("action"); // CLEAR or BLOCK
        FraudEventEntity updated = fraudDetectionService.processFraudAction(id, action);
        return ResponseEntity.ok(ApiResponse.success(updated, "Fraud event processed: " + action));
    }
}
