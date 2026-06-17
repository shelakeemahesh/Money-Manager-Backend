package in.maheshshelakee.moneymanager.controller;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.FriendExpenseDTO;
import in.maheshshelakee.moneymanager.dto.FriendStatsResponse;
import in.maheshshelakee.moneymanager.service.FriendExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friend-expenses")
@RequiredArgsConstructor
public class FriendExpenseController {

    private final FriendExpenseService friendExpenseService;

    private String getAuthenticatedUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendExpenseDTO>>> getAll() {
        List<FriendExpenseDTO> data = friendExpenseService.getAllFriendExpenses(getAuthenticatedUserEmail());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FriendExpenseDTO>> create(@Valid @RequestBody FriendExpenseDTO dto) {
        FriendExpenseDTO data = friendExpenseService.createFriendExpense(dto, getAuthenticatedUserEmail());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FriendExpenseDTO>> update(@PathVariable Long id, @Valid @RequestBody FriendExpenseDTO dto) {
        FriendExpenseDTO data = friendExpenseService.updateFriendExpense(id, dto, getAuthenticatedUserEmail());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        friendExpenseService.deleteFriendExpense(id, getAuthenticatedUserEmail());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<FriendStatsResponse>> getStats() {
        FriendStatsResponse data = friendExpenseService.getFriendStats(getAuthenticatedUserEmail());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
