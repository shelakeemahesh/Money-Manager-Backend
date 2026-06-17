package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.dto.PaginatedTransactionsResponse;
import in.maheshshelakee.moneymanager.dto.TransactionDto;
import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.IncomeEntity;
import in.maheshshelakee.moneymanager.entity.FriendExpense;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import in.maheshshelakee.moneymanager.repository.FriendExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTransactionService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final FriendExpenseRepository friendExpenseRepository;

    @Transactional(readOnly = true)
    public PaginatedTransactionsResponse getAllTransactions(
            int page, int size, Long userId, String category,
            LocalDate fromDate, LocalDate toDate, String type) {

        List<TransactionDto> combined = new ArrayList<>();

        // Fetch Expenses if type is ALL or EXPENSE
        if (type == null || "ALL".equalsIgnoreCase(type) || "EXPENSE".equalsIgnoreCase(type)) {
            List<ExpenseEntity> expenses = expenseRepository.findAll();
            for (ExpenseEntity e : expenses) {
                boolean flagged = Boolean.TRUE.equals(e.getFlagged())
                        || e.getAmount() > 50000
                        || (e.getNote() != null && e.getNote().toLowerCase().contains("suspicious"));

                combined.add(TransactionDto.builder()
                        .id(e.getId())
                        .type("EXPENSE")
                        .title(e.getTitle())
                        .amount(e.getAmount())
                        .category(e.getCategory())
                        .note(e.getNote())
                        .date(e.getExpenseDate())
                        .paymentMethod(e.getPaymentMethod())
                        .flagged(flagged)
                        .userId(e.getUser().getId())
                        .userName(e.getUser().getFullName())
                        .userEmail(e.getUser().getEmail())
                        .build());
            }

            List<FriendExpense> friendExpenses = friendExpenseRepository.findAll();
            for (FriendExpense fe : friendExpenses) {
                boolean flagged = fe.getAmount() > 50000;
                combined.add(TransactionDto.builder()
                        .id(fe.getId())
                        .type("EXPENSE")
                        .title("Spend on: " + fe.getFriendName())
                        .amount(fe.getAmount())
                        .category(fe.getCategory())
                        .note(fe.getDescription() != null && !fe.getDescription().isBlank() ? fe.getDescription() : "Spent on friend: " + fe.getFriendName())
                        .date(fe.getExpenseDate())
                        .paymentMethod("Friend Outflow")
                        .flagged(flagged)
                        .userId(fe.getUser().getId())
                        .userName(fe.getUser().getFullName())
                        .userEmail(fe.getUser().getEmail())
                        .build());
            }
        }

        // Fetch Incomes if type is ALL or INCOME
        if (type == null || "ALL".equalsIgnoreCase(type) || "INCOME".equalsIgnoreCase(type)) {
            List<IncomeEntity> incomes = incomeRepository.findAll();
            for (IncomeEntity i : incomes) {
                boolean flagged = Boolean.TRUE.equals(i.getFlagged())
                        || i.getAmount() > 50000
                        || (i.getSource() != null && i.getSource().toLowerCase().contains("suspicious"));

                combined.add(TransactionDto.builder()
                        .id(i.getId())
                        .type("INCOME")
                        .title(i.getSource())
                        .amount(i.getAmount())
                        .category(i.getCategory())
                        .note(i.getSource())
                        .date(i.getDate())
                        .paymentMethod("Standard Transfer")
                        .flagged(flagged)
                        .userId(i.getUser().getId())
                        .userName(i.getUser().getFullName())
                        .userEmail(i.getUser().getEmail())
                        .build());
            }
        }

        // Apply filters in memory
        List<TransactionDto> filtered = combined.stream()
                .filter(t -> userId == null || t.getUserId().equals(userId))
                .filter(t -> category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)
                        || t.getCategory().toLowerCase().contains(category.trim().toLowerCase()))
                .filter(t -> fromDate == null || !t.getDate().isBefore(fromDate))
                .filter(t -> toDate == null || !t.getDate().isAfter(toDate))
                .sorted(Comparator.comparing(TransactionDto::getDate).reversed()
                        .thenComparing(TransactionDto::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        int totalCount = filtered.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<TransactionDto> paginatedList = new ArrayList<>();
        if (fromIndex < totalCount) {
            paginatedList = filtered.subList(fromIndex, toIndex);
        }

        return PaginatedTransactionsResponse.builder()
                .transactions(paginatedList)
                .totalCount(totalCount)
                .page(page)
                .pageSize(size)
                .build();
    }
}
