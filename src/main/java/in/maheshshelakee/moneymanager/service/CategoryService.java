package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.dto.CategoryRequest;
import in.maheshshelakee.moneymanager.dto.CategoryResponse;
import in.maheshshelakee.moneymanager.dto.SubCategoryResponse;
import in.maheshshelakee.moneymanager.entity.CategoryEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.CategoryRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // ─── READ ──────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(String email) {
        User user = getUser(email);
        return categoryRepository.findActiveByUserOrGlobal(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── CREATE ─────────────────────────────────────────────────────────────────
    @Transactional
    public CategoryResponse create(CategoryRequest request, String email) {
        User user = getUser(email);
        String normalizedType = request.getType().toUpperCase();

        if (categoryRepository.existsByNameAndTypeAndUser(
                request.getName().trim(),
                normalizedType,
                user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category '" + request.getName() + "' already exists for type " + normalizedType);
        }

        CategoryEntity entity = CategoryEntity.builder()
                .name(request.getName().trim())
                .type(normalizedType)
                .icon(request.getIcon() != null && !request.getIcon().isBlank()
                        ? request.getIcon() : "📁")
                .color(request.getColor() != null && !request.getColor().isBlank()
                        ? request.getColor() : "#6366f1")
                .user(user)
                .build();

        return toResponse(categoryRepository.save(entity));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────────
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request, String email) {
        User user = getUser(email);

        CategoryEntity entity = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        String normalizedType = request.getType().toUpperCase();

        boolean nameChanged =
                !entity.getName().equalsIgnoreCase(request.getName().trim()) ||
                        !entity.getType().equalsIgnoreCase(normalizedType);

        if (nameChanged && categoryRepository.existsByNameAndTypeAndUser(
                request.getName().trim(),
                normalizedType,
                user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category '" + request.getName() + "' already exists for type " + normalizedType);
        }

        entity.setName(request.getName().trim());
        entity.setType(normalizedType);

        if (request.getIcon() != null && !request.getIcon().isBlank()) {
            entity.setIcon(request.getIcon());
        }

        if (request.getColor() != null && !request.getColor().isBlank()) {
            entity.setColor(request.getColor());
        }

        return toResponse(categoryRepository.save(entity));
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, String email) {
        User user = getUser(email);

        CategoryEntity entity = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        categoryRepository.delete(entity);
    }

    // ─── DEFAULT CATEGORIES ─────────────────────────────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDefaults(User user) {

        List<Object[]> defaults = List.of(
                new Object[] { "Salary",        "INCOME",  "💼", "#22c55e" },
                new Object[] { "Freelance",     "INCOME",  "🖥️", "#10b981" },
                new Object[] { "Investments",   "INCOME",  "📈", "#06b6d4" },
                new Object[] { "Other Income",  "INCOME",  "💰", "#6366f1" },
                new Object[] { "Food",          "EXPENSE", "🍔", "#ef4444" },
                new Object[] { "Transport",     "EXPENSE", "🚗", "#f97316" },
                new Object[] { "Shopping",      "EXPENSE", "🛍️", "#8b5cf6" },
                new Object[] { "Health",        "EXPENSE", "🏥", "#ec4899" },
                new Object[] { "Utilities",     "EXPENSE", "💡", "#f59e0b" },
                new Object[] { "Entertainment", "EXPENSE", "🎬", "#14b8a6" },
                new Object[] { "Education",     "EXPENSE", "📚", "#3b82f6" },
                new Object[] { "Other",         "EXPENSE", "📦", "#6b7280" }
        );

        List<CategoryEntity> entities = defaults.stream()
                .map(row -> CategoryEntity.builder()
                        .name((String) row[0])
                        .type((String) row[1])
                        .icon((String) row[2])
                        .color((String) row[3])
                        .user(user)
                        .build())
                .collect(Collectors.toList());

        categoryRepository.saveAll(entities);
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────────
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    public CategoryResponse toResponse(CategoryEntity entity) {

        List<SubCategoryResponse> subs = entity.getSubcategories()
                .stream()
                .map(s -> SubCategoryResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .icon(s.getIcon())
                        .categoryId(entity.getId())
                        .categoryName(entity.getName())
                        .createdAt(s.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .icon(entity.getIcon())
                .color(entity.getColor())
                .subcategoryCount(subs.size())
                .subcategories(subs)
                .globalTemplate(entity.getGlobalTemplate())
                .archived(entity.getArchived())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public CategoryEntity getCategoryByIdAndEmail(Long id, String email) {
        User user = getUser(email);

        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
}