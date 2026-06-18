package in.maheshshelakee.moneymanager.controller.user;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.SubCategoryRequest;
import in.maheshshelakee.moneymanager.dto.SubCategoryResponse;
import in.maheshshelakee.moneymanager.service.user.SubCategoryService;
import in.maheshshelakee.moneymanager.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subcategories")
@RequiredArgsConstructor
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubCategoryResponse>> create(
            @Valid @RequestBody SubCategoryRequest request) {
        SubCategoryResponse created = subCategoryService.create(request, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Subcategory created successfully"));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<List<SubCategoryResponse>>> getByCategoryId(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                subCategoryService.getByCategoryId(categoryId, SecurityUtils.getCurrentUserEmail())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subCategoryService.delete(id, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }
}
