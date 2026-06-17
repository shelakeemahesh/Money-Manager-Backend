package in.maheshshelakee.moneymanager.controller;

import in.maheshshelakee.moneymanager.dto.*;
import in.maheshshelakee.moneymanager.entity.FriendTransactionStatus;
import in.maheshshelakee.moneymanager.entity.FriendTransactionType;
import in.maheshshelakee.moneymanager.service.FriendTransactionService;
import in.maheshshelakee.moneymanager.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friend-transactions")
@RequiredArgsConstructor
public class FriendTransactionController {

    private final FriendTransactionService friendTransactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<FriendTransactionResponseDTO>> create(
            @Valid @RequestBody FriendTransactionRequestDTO request) {
        FriendTransactionResponseDTO created = friendTransactionService.createTransaction(
                request, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Friend transaction created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FriendTransactionResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody FriendTransactionRequestDTO request) {
        FriendTransactionResponseDTO updated = friendTransactionService.updateTransaction(
                id, request, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(updated, "Friend transaction updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        friendTransactionService.deleteTransaction(id, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendTransactionResponseDTO>>> getAll(
            @RequestParam(required = false) FriendTransactionType type,
            @RequestParam(required = false) FriendTransactionStatus status,
            @RequestParam(required = false) String friendName) {
        List<FriendTransactionResponseDTO> transactions = friendTransactionService.getAllByUser(
                SecurityUtils.getCurrentUserEmail(), type, status, friendName);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PatchMapping("/{id}/settle")
    public ResponseEntity<ApiResponse<FriendTransactionResponseDTO>> settle(@PathVariable Long id) {
        FriendTransactionResponseDTO settled = friendTransactionService.markAsSettled(
                id, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(settled, "Transaction marked as settled successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<OverallSummaryDTO>> getOverallSummary() {
        OverallSummaryDTO summary = friendTransactionService.getOverallSummary(
                SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/summary/{friendName}")
    public ResponseEntity<ApiResponse<FriendSummaryDTO>> getFriendSummary(@PathVariable String friendName) {
        FriendSummaryDTO summary = friendTransactionService.getFriendSummary(
                friendName, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
