package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.ApiResponse;
import in.maheshshelakee.moneymanager.entity.NotificationEntity;
import in.maheshshelakee.moneymanager.entity.NotificationTemplateEntity;
import in.maheshshelakee.moneymanager.service.admin.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationEntity>> queueNotification(@RequestBody NotificationEntity notification) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.queueNotification(notification)));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<NotificationEntity>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationLogs(PageRequest.of(page, size))));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<NotificationTemplateEntity>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getTemplates()));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<NotificationTemplateEntity>> saveTemplate(@RequestBody NotificationTemplateEntity template) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.saveTemplate(template)));
    }
}
