package in.maheshshelakee.moneymanager.service.user;

import in.maheshshelakee.moneymanager.dto.ExpenseRequest;
import in.maheshshelakee.moneymanager.dto.ExpenseResponse;
import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.exception.ResourceNotFoundException;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAll(String email) {
        User user = userService.getUserByEmail(email);
        return expenseRepository.findByUserOrderByExpenseDateDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        ExpenseEntity entity = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        return toResponse(entity);
    }

    @Transactional
    public ExpenseResponse add(ExpenseRequest request, String email) {
        User user = userService.getUserByEmail(email);
        ExpenseEntity entity = ExpenseEntity.builder()
                .title(request.getTitle().trim())
                .amount(request.getAmount())
                .category(request.getCategory().trim())
                .note(request.getNote())
                .expenseDate(request.getExpenseDate())
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .icon(resolveIcon(request.getIcon()))
                .user(user)
                .build();
        return toResponse(expenseRepository.save(entity));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request, String email) {
        User user = userService.getUserByEmail(email);
        ExpenseEntity entity = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));

        entity.setTitle(request.getTitle().trim());
        entity.setAmount(request.getAmount());
        entity.setCategory(request.getCategory().trim());
        entity.setNote(request.getNote());
        entity.setExpenseDate(request.getExpenseDate());
        entity.setPaymentMethod(request.getPaymentMethod().toUpperCase());
        if (request.getIcon() != null && !request.getIcon().isBlank()) {
            entity.setIcon(request.getIcon());
        }
        return toResponse(expenseRepository.save(entity));
    }

    @Transactional
    public void delete(Long id, String email) {
        User user = userService.getUserByEmail(email);
        ExpenseEntity entity = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        expenseRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByCategory(String category, String email) {
        User user = userService.getUserByEmail(email);
        return expenseRepository
                .findByUserAndCategoryIgnoreCaseOrderByExpenseDateDesc(user, category)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByDateRange(LocalDate startDate, LocalDate endDate, String email) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        User user = userService.getUserByEmail(email);
        return expenseRepository
                .findByUserAndExpenseDateBetweenOrderByExpenseDateDesc(user, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getTotalAmount(String email) {
        User user = userService.getUserByEmail(email);
        // FIX: Removed the redundant null-check. The JPQL query already uses COALESCE which
        //      guarantees a non-null result. One authoritative null-guard is cleaner than two.
        return expenseRepository.sumAmountByUser(user);
    }

    public ExpenseResponse toResponse(ExpenseEntity entity) {
        return ExpenseResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .amount(entity.getAmount())
                .category(entity.getCategory())
                .note(entity.getNote())
                .date(entity.getExpenseDate())
                .paymentMethod(entity.getPaymentMethod())
                .icon(entity.getIcon())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String resolveIcon(String icon) {
        return (icon != null && !icon.isBlank()) ? icon : "🛒";
    }
}
