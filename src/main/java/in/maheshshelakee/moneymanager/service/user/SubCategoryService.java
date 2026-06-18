package in.maheshshelakee.moneymanager.service.user;

import in.maheshshelakee.moneymanager.dto.SubCategoryRequest;
import in.maheshshelakee.moneymanager.dto.SubCategoryResponse;
import in.maheshshelakee.moneymanager.entity.CategoryEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.SubCategoryEntity;
import in.maheshshelakee.moneymanager.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryService categoryService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getByCategoryId(Long categoryId, String email) {
        CategoryEntity category = categoryService.getCategoryByIdAndEmail(categoryId, email);
        return subCategoryRepository.findByCategoryOrderByCreatedAtDesc(category)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public SubCategoryResponse create(SubCategoryRequest request, String email) {
        CategoryEntity category = categoryService.getCategoryByIdAndEmail(request.getCategoryId(), email);
        SubCategoryEntity entity = SubCategoryEntity.builder()
                .name(request.getName().trim())
                .icon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon() : "📌")
                .category(category)
                .build();
        return toResponse(subCategoryRepository.save(entity));
    }

    @Transactional
    public void delete(Long id, String email) {
        User user = userService.getUserByEmail(email);
        SubCategoryEntity entity = subCategoryRepository.findByIdAndCategoryUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subcategory not found"));
        subCategoryRepository.delete(entity);
    }

    private SubCategoryResponse toResponse(SubCategoryEntity entity) {
        return SubCategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
