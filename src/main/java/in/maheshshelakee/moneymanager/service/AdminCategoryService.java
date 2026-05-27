package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.dto.CategoryRequest;
import in.maheshshelakee.moneymanager.dto.CategoryResponse;
import in.maheshshelakee.moneymanager.entity.CategoryEntity;
import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.IncomeEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.CategoryRepository;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findActiveGlobals().stream()
                .map(categoryService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        validateRequest(request);

        boolean exists = categoryRepository.findActiveGlobals().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(request.getName().trim())
                        && c.getType().equalsIgnoreCase(request.getType()));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Global category '" + request.getName() + "' already exists for type " + request.getType());
        }

        CategoryEntity entity = CategoryEntity.builder()
                .name(request.getName().trim())
                .type(request.getType().toUpperCase())
                .icon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon() : "📁")
                .color(request.getColor() != null && !request.getColor().isBlank() ? request.getColor() : "#6366f1")
                .globalTemplate(Boolean.TRUE.equals(request.getGlobalTemplate()))
                .archived(false)
                .user(null)
                .build();

        CategoryEntity saved = categoryRepository.save(entity);

        if (Boolean.TRUE.equals(saved.getGlobalTemplate())) {
            propagateCategoryToAllUsers(saved);
        }

        return categoryService.toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        validateRequest(request);

        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Global category not found"));

        if (entity.getUser() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update user category as global");
        }

        boolean nameChanged = !entity.getName().equalsIgnoreCase(request.getName().trim())
                || !entity.getType().equalsIgnoreCase(request.getType());

        if (nameChanged) {
            boolean exists = categoryRepository.findActiveGlobals().stream()
                    .anyMatch(c -> !c.getId().equals(id)
                            && c.getName().equalsIgnoreCase(request.getName().trim())
                            && c.getType().equalsIgnoreCase(request.getType()));
            if (exists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Global category '" + request.getName() + "' already exists for type " + request.getType());
            }
        }

        entity.setName(request.getName().trim());
        entity.setType(request.getType().toUpperCase());
        entity.setIcon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon() : "📁");
        entity.setColor(request.getColor() != null && !request.getColor().isBlank() ? request.getColor() : "#6366f1");

        boolean oldGlobalTemplate = Boolean.TRUE.equals(entity.getGlobalTemplate());
        entity.setGlobalTemplate(Boolean.TRUE.equals(request.getGlobalTemplate()));

        CategoryEntity saved = categoryRepository.save(entity);

        // Propagate if toggled from false to true
        if (Boolean.TRUE.equals(saved.getGlobalTemplate()) && !oldGlobalTemplate) {
            propagateCategoryToAllUsers(saved);
        }

        return categoryService.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Global category not found"));

        if (entity.getUser() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete user category via admin");
        }

        // Check if in use in transactions
        boolean inUseInExpenses = expenseRepository.findAll().stream()
                .anyMatch(e -> e.getCategory().equalsIgnoreCase(entity.getName()));
        boolean inUseInIncomes = incomeRepository.findAll().stream()
                .anyMatch(i -> i.getCategory().equalsIgnoreCase(entity.getName()));

        if (inUseInExpenses || inUseInIncomes) {
            entity.setArchived(true);
            categoryRepository.save(entity);
        } else {
            categoryRepository.delete(entity);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAnalytics() {
        LocalDate since = LocalDate.now().minusDays(30);

        List<ExpenseEntity> expenses = expenseRepository.findAll().stream()
                .filter(e -> !e.getExpenseDate().isBefore(since))
                .collect(Collectors.toList());

        List<IncomeEntity> incomes = incomeRepository.findAll().stream()
                .filter(i -> i.getDate() != null && !i.getDate().isBefore(since))
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> summary = new HashMap<>();

        for (ExpenseEntity e : expenses) {
            String cat = e.getCategory();
            summary.putIfAbsent(cat, new HashMap<>(Map.of("category", cat, "count", 0L, "amount", 0.0)));
            Map<String, Object> data = summary.get(cat);
            data.put("count", (long) data.get("count") + 1);
            data.put("amount", (double) data.get("amount") + e.getAmount());
        }

        for (IncomeEntity i : incomes) {
            String cat = i.getCategory();
            summary.putIfAbsent(cat, new HashMap<>(Map.of("category", cat, "count", 0L, "amount", 0.0)));
            Map<String, Object> data = summary.get(cat);
            data.put("count", (long) data.get("count") + 1);
            data.put("amount", (double) data.get("amount") + i.getAmount());
        }

        return new ArrayList<>(summary.values());
    }

    private void validateRequest(CategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is required");
        }
        if (request.getName().trim().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name must be maximum 50 characters");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category type is required");
        }
        if (!"INCOME".equalsIgnoreCase(request.getType()) && !"EXPENSE".equalsIgnoreCase(request.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category type must be INCOME or EXPENSE");
        }
    }

    private void propagateCategoryToAllUsers(CategoryEntity globalCat) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            boolean exists = categoryRepository.existsByNameAndTypeAndUser(
                    globalCat.getName(), globalCat.getType(), user);
            if (!exists) {
                CategoryEntity clone = CategoryEntity.builder()
                        .name(globalCat.getName())
                        .type(globalCat.getType())
                        .icon(globalCat.getIcon())
                        .color(globalCat.getColor())
                        .user(user)
                        .globalTemplate(false)
                        .archived(false)
                        .build();
                categoryRepository.save(clone);
            }
        }
    }
}
