package in.maheshshelakee.moneymanager.controller.admin;

import in.maheshshelakee.moneymanager.dto.*;
import in.maheshshelakee.moneymanager.entity.ReportEntity;
import in.maheshshelakee.moneymanager.service.admin.AdminSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
public class AdminSupportController {

    private final AdminSupportService supportService;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicketDto>>> getAllTickets() {
        List<SupportTicketDto> tickets = supportService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<SupportTicketDto>> getTicket(@PathVariable Long id) {
        SupportTicketDto ticket = supportService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping("/tickets/{id}/replies")
    public ResponseEntity<ApiResponse<List<TicketReplyDto>>> getReplies(@PathVariable Long id) {
        List<TicketReplyDto> replies = supportService.getTicketReplies(id);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<ApiResponse<TicketReplyDto>> submitReply(
            @PathVariable Long id, @RequestBody TicketReplyRequest request, Principal principal) {
        
        String senderEmail = principal != null ? principal.getName() : "admin@moneymanager.com";
        TicketReplyDto reply = supportService.submitReply(id, senderEmail, request.getMessage());
        return ResponseEntity.ok(ApiResponse.success(reply));
    }

    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<ApiResponse<SupportTicketDto>> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        
        String status = payload.get("status");
        SupportTicketDto ticket = supportService.updateTicketStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<List<UserFeedbackDto>>> getFeedback() {
        List<UserFeedbackDto> feedbacks = supportService.getAllFeedback();
        return ResponseEntity.ok(ApiResponse.success(feedbacks));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<SupportAnalyticsResponse>> getAnalytics() {
        SupportAnalyticsResponse analytics = supportService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
