package in.maheshshelakee.moneymanager.controller.user;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.dto.FilterResultDTO;
import in.maheshshelakee.moneymanager.service.user.FilterService;
import in.maheshshelakee.moneymanager.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/filter")
@RequiredArgsConstructor
public class FilterController {

    private final FilterService filterService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FilterResultDTO>>> filter(
            @RequestParam(defaultValue = "Income") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "Date") String sortField,
            @RequestParam(defaultValue = "Descending") String sortOrder,
            @RequestParam(defaultValue = "") String search) {

        List<FilterResultDTO> results = filterService.filter(
                SecurityUtils.getCurrentUserEmail(), type, startDate, endDate, sortField, sortOrder, search);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
