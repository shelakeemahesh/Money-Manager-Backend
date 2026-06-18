package in.maheshshelakee.moneymanager.service.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationDeliveryAdapter {

    public boolean sendEmail(String to, String subject, String body) {
        log.info("[Simulation] Sending EMAIL via SendGrid to: {}. Subject: '{}'", to, subject);
        // Simulate network call
        try {
            Thread.sleep(100);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public boolean sendPushNotification(String topicOrToken, String subject, String body) {
        log.info("[Simulation] Sending PUSH via FCM to: {}. Title: '{}'", topicOrToken, subject);
        try {
            Thread.sleep(100);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
