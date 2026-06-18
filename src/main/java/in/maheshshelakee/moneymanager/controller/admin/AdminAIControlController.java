package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.entity.AILogEntity;
import in.maheshshelakee.moneymanager.entity.AISettingsEntity;
import in.maheshshelakee.moneymanager.service.admin.AdminAIControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AdminAIControlController {

    private final AdminAIControlService adminAIControlService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminAIControlService.getDashboardMetrics()));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AISettingsEntity>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(adminAIControlService.getSettings()));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AISettingsEntity>> updateSettings(@RequestBody AISettingsEntity newSettings) {
        return ResponseEntity.ok(ApiResponse.success(adminAIControlService.updateSettings(newSettings)));
    }

    @PostMapping("/reanalyze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reanalyze() {
        return ResponseEntity.ok(ApiResponse.success(adminAIControlService.reanalyzeData()));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AILogEntity>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(adminAIControlService.getLogs(page, size)));
    }
}
