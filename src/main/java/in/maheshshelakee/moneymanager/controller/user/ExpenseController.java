package in.maheshshelakee.moneymanager.controller.user;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.ExpenseRequest;
import in.maheshshelakee.moneymanager.dto.ExpenseResponse;
import in.maheshshelakee.moneymanager.service.user.ExpenseService;
import in.maheshshelakee.moneymanager.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getAll(SecurityUtils.getCurrentUserEmail())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getById(id, SecurityUtils.getCurrentUserEmail())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = expenseService.add(request, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Expense created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(@PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.update(id, request, SecurityUtils.getCurrentUserEmail())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getByCategory(@RequestParam String category) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getByCategory(category, SecurityUtils.getCurrentUserEmail())));
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                expenseService.getByDateRange(startDate, endDate, SecurityUtils.getCurrentUserEmail())));
    }

    @GetMapping("/total")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getTotal() {
        Double total = expenseService.getTotalAmount(SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(Map.of("totalExpense", total)));
    }
}
