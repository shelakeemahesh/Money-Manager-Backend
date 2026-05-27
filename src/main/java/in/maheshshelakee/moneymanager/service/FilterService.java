package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.dto.FilterResultDTO;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilterService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<FilterResultDTO> filter(String email, String type, LocalDate startDate, LocalDate endDate,
            String sortField, String sortOrder, String search) {

        User user = userService.getUserByEmail(email);
        List<FilterResultDTO> results;

        if ("Income".equalsIgnoreCase(type)) {
            var incomes = (startDate != null && endDate != null)
                    ? incomeRepository.findByUserAndDateBetweenOrderByDateDesc(user, startDate, endDate)
                    : incomeRepository.findByUserOrderByDateDesc(user);

            results = incomes.stream()
                    .map(i -> FilterResultDTO.builder()
                            .id(i.getId())
                            .label(i.getSource())
                            .type("Income")
                            .amount(i.getAmount())
                            .date(i.getDate())
                            .icon(i.getIcon())
                            .build())
                    .collect(Collectors.toList());

        } else if ("Expense".equalsIgnoreCase(type)) {
            var expenses = (startDate != null && endDate != null)
                    ? expenseRepository.findByUserAndExpenseDateBetweenOrderByExpenseDateDesc(
                            user, startDate, endDate)
                    : expenseRepository.findByUserOrderByExpenseDateDesc(user);

            results = expenses.stream()
                    .map(e -> FilterResultDTO.builder()
                            .id(e.getId())
                            .label(e.getTitle() != null ? e.getTitle() : e.getCategory())
                            .type("Expense")
                            .amount(e.getAmount())
                            .date(e.getExpenseDate())
                            .icon(e.getIcon())
                            .note(e.getNote())
                            .paymentMethod(e.getPaymentMethod())
                            .build())
                    .collect(Collectors.toList());

        } else {
            // FIX: Previously any unknown type silently returned income results.
            //      Now we fail fast with a clear 400.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid type '" + type + "'. Must be 'Income' or 'Expense'.");
        }

        // Keyword search
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            results = results.stream()
                    .filter(r -> r.getLabel() != null && r.getLabel().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        // FIX: Use explicit generic type on comparator to avoid unchecked-cast issues.
        //      Added guard for unknown sortField values — previously an unknown value
        //      silently fell through to date sort with no error or warning.
        Comparator<FilterResultDTO> comparator;
        if ("Amount".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparingDouble(
                    (FilterResultDTO r) -> r.getAmount() != null ? r.getAmount() : 0.0);
        } else if ("Date".equalsIgnoreCase(sortField) || sortField == null || sortField.isBlank()) {
            comparator = Comparator.comparing(
                    (FilterResultDTO r) -> r.getDate() != null ? r.getDate() : LocalDate.MIN);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid sortField '" + sortField + "'. Must be 'Date' or 'Amount'.");
        }

        if ("Descending".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        return results.stream().sorted(comparator).collect(Collectors.toList());
    }
}
