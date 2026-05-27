package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.entity.NotificationEntity;
import in.maheshshelakee.moneymanager.entity.NotificationTemplateEntity;
import in.maheshshelakee.moneymanager.repository.NotificationRepository;
import in.maheshshelakee.moneymanager.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryAdapter deliveryAdapter;

    @Transactional
    public NotificationEntity queueNotification(NotificationEntity notification) {
        notification.setStatus("PENDING");
        if (notification.getScheduledAt() == null) {
            notification.setScheduledAt(LocalDateTime.now());
        }
        
        // Estimate recipient count
        if ("ALL".equals(notification.getRecipientType())) {
            notification.setRecipientCount(150); // Simulated count
        } else {
            notification.setRecipientCount(1);
        }
        
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> getNotificationLogs(Pageable pageable) {
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateEntity> getTemplates() {
        return templateRepository.findAll();
    }
    
    @Transactional
    public NotificationTemplateEntity saveTemplate(NotificationTemplateEntity template) {
        return templateRepository.save(template);
    }

    // Cron job running every 30 seconds to process pending notifications
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processPendingNotifications() {
        List<NotificationEntity> pending = notificationRepository.findByStatusAndScheduledAtBefore("PENDING", LocalDateTime.now());
        if (pending.isEmpty()) return;

        log.info("Processing {} pending notifications...", pending.size());

        for (NotificationEntity notification : pending) {
            boolean success = false;
            
            if ("EMAIL".equals(notification.getType())) {
                success = deliveryAdapter.sendEmail(notification.getRecipientValue(), notification.getSubject(), notification.getMessageBody());
            } else if ("PUSH".equals(notification.getType())) {
                success = deliveryAdapter.sendPushNotification(notification.getRecipientValue(), notification.getSubject(), notification.getMessageBody());
            } else {
                success = true; // In-app just saves to DB for the user to read
            }

            if (success) {
                notification.setStatus("DELIVERED");
                notification.setSentAt(LocalDateTime.now());
            } else {
                int retries = notification.getRetryCount() + 1;
                notification.setRetryCount(retries);
                if (retries >= 3) {
                    notification.setStatus("FAILED");
                }
            }
            notificationRepository.save(notification);
        }
    }
}
