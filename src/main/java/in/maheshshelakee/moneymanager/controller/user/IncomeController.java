package in.maheshshelakee.moneymanager.controller.user;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.IncomeDTO;
import in.maheshshelakee.moneymanager.service.user.IncomeService;
import in.maheshshelakee.moneymanager.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/income")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncomeDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                incomeService.getAll(SecurityUtils.getCurrentUserEmail())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IncomeDTO>> add(@Valid @RequestBody IncomeDTO dto) {
        IncomeDTO created = incomeService.add(dto, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Income added successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeDTO>> update(@PathVariable Long id,
            @Valid @RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                incomeService.update(id, dto, SecurityUtils.getCurrentUserEmail())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        incomeService.delete(id, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }
}
