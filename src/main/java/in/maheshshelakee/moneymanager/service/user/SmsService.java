package in.maheshshelakee.moneymanager.service.user;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    private boolean isTwilioConfigured = false;

    /**
     * Initialize Twilio SDK only if valid credentials are provided.
     */
    @PostConstruct
    public void initTwilio() {
        if (accountSid != null && !accountSid.isBlank() && !accountSid.startsWith("your_")
                && authToken != null && !authToken.isBlank() && !authToken.startsWith("your_")) {
            try {
                Twilio.init(accountSid, authToken);
                isTwilioConfigured = true;
                log.info("Twilio SDK initialized successfully with Account SID: {}", accountSid);
            } catch (Exception e) {
                log.error("Failed to initialize Twilio SDK: {}", e.getMessage());
            }
        } else {
            log.warn("Twilio credentials are not configured or hold default placeholder values. Falling back to console logs for SMS dispatch.");
        }
    }

    /**
     * Send SMS containing OTP to the user's phone number.
     * Uses Twilio SMS API if configured; otherwise, falls back to logging the code.
     *
     * @param to      the recipient phone number
     * @param message the text message to send
     */
    public void sendSms(String to, String message) {
        if (isTwilioConfigured) {
            try {
                Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(twilioPhoneNumber),
                        message
                ).create();
                log.info("Real SMS successfully sent via Twilio to {}", to);
            } catch (Exception e) {
                log.error("Failed to dispatch real SMS via Twilio to {}: {}", to, e.getMessage());
                logFallbackSms(to, message);
            }
        } else {
            logFallbackSms(to, message);
        }
    }

    private void logFallbackSms(String to, String message) {
        log.info("--------------------------------------------------");
        log.info("SMS CONSOLE LOG (Twilio not active):");
        log.info("To: {}", to);
        log.info("Message: {}", message);
        log.info("--------------------------------------------------");
    }
}
