package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.entity.SystemErrorLog;
import in.maheshshelakee.moneymanager.service.admin.SystemMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/monitoring")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMonitoringController {

    private final SystemMonitoringService monitoringService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.getMonitoringStats()));
    }

    @GetMapping("/request-volume")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRequestVolume() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.getRequestVolumePerMinute()));
    }

    @GetMapping("/errors")
    public ResponseEntity<ApiResponse<Page<SystemErrorLog>>> getErrors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(ApiResponse.success(monitoringService.getPaginatedErrors(pageable)));
    }

    @GetMapping("/slow-queries")
    public ResponseEntity<ApiResponse<List<SystemMonitoringService.SlowQueryLog>>> getSlowQueries() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.getSlowQueries()));
    }
}
