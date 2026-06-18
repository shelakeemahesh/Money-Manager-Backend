package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.CategoryRequest;
import in.maheshshelakee.moneymanager.dto.CategoryResponse;
import in.maheshshelakee.moneymanager.service.admin.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllGlobalCategories() {
        List<CategoryResponse> categories = adminCategoryService.getAll();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createGlobalCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = adminCategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateGlobalCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = adminCategoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGlobalCategory(@PathVariable Long id) {
        adminCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted or archived successfully"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryAnalytics() {
        List<Map<String, Object>> stats = adminCategoryService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
