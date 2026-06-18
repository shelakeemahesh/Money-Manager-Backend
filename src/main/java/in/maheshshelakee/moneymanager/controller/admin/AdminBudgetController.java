package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.BudgetDto;
import in.maheshshelakee.moneymanager.dto.HeatmapDto;
import in.maheshshelakee.moneymanager.service.admin.AdminBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/budgets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBudgetController {

    private final AdminBudgetService adminBudgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BudgetDto>>> getAllBudgets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<BudgetDto> budgets = adminBudgetService.getAllBudgets(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<List<HeatmapDto>>> getBudgetHeatmap() {
        List<HeatmapDto> heatmap = adminBudgetService.getBudgetHeatmap();
        return ResponseEntity.ok(ApiResponse.success(heatmap));
    }

    @PostMapping("/ai-recommendation/{userId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateAIRecommendation(@PathVariable Long userId) {
        String recommendation = adminBudgetService.generateAIRecommendation(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("recommendation", recommendation)));
    }
}
