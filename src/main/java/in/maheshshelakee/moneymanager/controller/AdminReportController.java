package in.maheshshelakee.moneymanager.controller;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.entity.ReportEntity;
import in.maheshshelakee.moneymanager.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService reportService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ReportEntity>> generateReport(
            @RequestBody Map<String, Object> payload, Principal principal) {
        
        String type = (String) payload.get("type");
        String fromDate = (String) payload.get("fromDate");
        String toDate = (String) payload.get("toDate");
        
        // Optional filters
        Long userId = null;
        if (payload.get("userId") != null) {
            userId = Long.valueOf(payload.get("userId").toString());
        }
        
        String category = (String) payload.get("category");
        String format = (String) payload.get("format");
        
        String adminEmail = principal != null ? principal.getName() : "ADMIN";
        
        ReportEntity report = reportService.triggerReportGeneration(
                type, fromDate, toDate, userId, category, format, adminEmail
        );
        
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ReportEntity>>> getHistory() {
        List<ReportEntity> history = reportService.getReportHistory();
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<StreamingResponseBody> downloadReport(@PathVariable Long id) {
        ReportEntity report = reportService.getReportById(id)
                .orElseThrow(() -> new NoSuchElementException("Report not found"));
        
        File file = new File(report.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        };
        
        String mimeType = report.getFormat().equalsIgnoreCase("PDF") ? "application/pdf" : "text/csv";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(responseBody);
    }
}
