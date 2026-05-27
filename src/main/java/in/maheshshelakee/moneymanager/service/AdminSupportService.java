package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.dto.*;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.SupportTicketEntity;
import in.maheshshelakee.moneymanager.entity.TicketReplyEntity;
import in.maheshshelakee.moneymanager.entity.UserFeedbackEntity;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import in.maheshshelakee.moneymanager.repository.SupportTicketRepository;
import in.maheshshelakee.moneymanager.repository.TicketReplyRepository;
import in.maheshshelakee.moneymanager.repository.UserFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSupportService {

    private final SupportTicketRepository ticketRepository;
    private final TicketReplyRepository replyRepository;
    private final UserFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<SupportTicketDto> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public SupportTicketDto getTicketById(Long id) {
        SupportTicketEntity ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found"));
        return convertToDto(ticket);
    }

    public List<TicketReplyDto> getTicketReplies(Long ticketId) {
        return replyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::convertReplyToDto)
                .collect(Collectors.toList());
    }

    public SupportTicketDto createTicket(String userEmail, String subject, String category, String priority) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .user(user)
                .subject(subject)
                .category(category)
                .priority(priority)
                .status("OPEN")
                .build();

        ticket = ticketRepository.save(ticket);
        SupportTicketDto dto = convertToDto(ticket);
        
        // Broadcast new ticket to support board
        messagingTemplate.convertAndSend("/topic/tickets", dto);
        return dto;
    }

    public TicketReplyDto submitReply(Long ticketId, String senderEmail, String message) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found"));

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new NoSuchElementException("Sender profile not found"));

        TicketReplyEntity reply = TicketReplyEntity.builder()
                .ticket(ticket)
                .sender(sender)
                .message(message)
                .build();

        reply = replyRepository.save(reply);
        
        // Automatically mark ticket as IN_PROGRESS if admin replies
        if (ticket.getStatus().equals("OPEN") && !sender.getRole().name().equals("USER")) {
            ticket.setStatus("IN_PROGRESS");
            ticketRepository.save(ticket);
        }

        TicketReplyDto replyDto = convertReplyToDto(reply);
        
        // Broadcast to dynamic topic (supports live update on client view)
        messagingTemplate.convertAndSend("/topic/tickets/" + ticketId, replyDto);
        
        // Broadcast overview list updates too
        messagingTemplate.convertAndSend("/topic/tickets", convertToDto(ticket));

        return replyDto;
    }

    public SupportTicketDto updateTicketStatus(Long ticketId, String status) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found"));

        ticket.setStatus(status);
        if (status.equalsIgnoreCase("RESOLVED") || status.equalsIgnoreCase("CLOSED")) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else {
            ticket.setResolvedAt(null);
        }

        ticket = ticketRepository.save(ticket);
        SupportTicketDto dto = convertToDto(ticket);
        
        // Push status changes over WebSocket
        messagingTemplate.convertAndSend("/topic/tickets", dto);
        
        return dto;
    }

    public List<UserFeedbackDto> getAllFeedback() {
        return feedbackRepository.findAll().stream()
                .map(this::convertFeedbackToDto)
                .collect(Collectors.toList());
    }

    public void submitFeedback(String userEmail, Integer rating, String comment) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        UserFeedbackEntity feedback = UserFeedbackEntity.builder()
                .user(user)
                .rating(rating)
                .comment(comment)
                .build();

        feedbackRepository.save(feedback);
    }

    public SupportAnalyticsResponse getAnalytics() {
        List<SupportTicketEntity> tickets = ticketRepository.findAll();
        
        // Calculate average resolution time (hours)
        double totalHours = 0;
        int resolvedCount = 0;
        for (SupportTicketEntity t : tickets) {
            if (t.getResolvedAt() != null && t.getCreatedAt() != null) {
                Duration duration = Duration.between(t.getCreatedAt(), t.getResolvedAt());
                totalHours += duration.toMinutes() / 60.0;
                resolvedCount++;
            }
        }
        double avgResolutionTime = resolvedCount > 0 ? (totalHours / resolvedCount) : 0.0;

        // Group categories
        Map<String, Long> categoryMap = tickets.stream()
                .collect(Collectors.groupingBy(SupportTicketEntity::getCategory, Collectors.counting()));

        // Group open vs resolved trend (daily counts)
        Map<String, Long> trendMap = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (SupportTicketEntity t : tickets) {
            if (t.getCreatedAt() != null) {
                String dateStr = t.getCreatedAt().format(formatter);
                trendMap.put(dateStr, trendMap.getOrDefault(dateStr, 0L) + 1L);
            }
        }

        return SupportAnalyticsResponse.builder()
                .averageResolutionTimeHours(avgResolutionTime)
                .ticketsByCategory(categoryMap)
                .openVsResolvedTrend(trendMap)
                .build();
    }

    private SupportTicketDto convertToDto(SupportTicketEntity ticket) {
        return SupportTicketDto.builder()
                .id(ticket.getId())
                .userEmail(ticket.getUser() != null ? ticket.getUser().getEmail() : "N/A")
                .userName(ticket.getUser() != null ? ticket.getUser().getFullName() : "N/A")
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }

    private TicketReplyDto convertReplyToDto(TicketReplyEntity reply) {
        return TicketReplyDto.builder()
                .id(reply.getId())
                .ticketId(reply.getTicket() != null ? reply.getTicket().getId() : null)
                .senderEmail(reply.getSender() != null ? reply.getSender().getEmail() : "N/A")
                .senderName(reply.getSender() != null ? reply.getSender().getFullName() : "N/A")
                .senderRole(reply.getSender() != null ? reply.getSender().getRole().name() : "USER")
                .message(reply.getMessage())
                .createdAt(reply.getCreatedAt())
                .build();
    }

    private UserFeedbackDto convertFeedbackToDto(UserFeedbackEntity feedback) {
        return UserFeedbackDto.builder()
                .id(feedback.getId())
                .userEmail(feedback.getUser() != null ? feedback.getUser().getEmail() : "N/A")
                .userName(feedback.getUser() != null ? feedback.getUser().getFullName() : "N/A")
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
