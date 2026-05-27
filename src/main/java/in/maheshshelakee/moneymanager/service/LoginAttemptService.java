package in.maheshshelakee.moneymanager.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int COOLDOWN_MINUTES = 15;

    private final ConcurrentHashMap<String, AttemptTracker> attemptsMap = new ConcurrentHashMap<>();

    public void loginFailed(String identifier) {
        attemptsMap.compute(identifier, (key, val) -> {
            if (val == null) {
                return new AttemptTracker(1, LocalDateTime.now());
            }
            if (val.getLockoutExpiry() != null && val.getLockoutExpiry().isAfter(LocalDateTime.now())) {
                return val;
            }
            int newAttempts = val.getAttempts() + 1;
            LocalDateTime lockoutExpiry = null;
            if (newAttempts >= MAX_ATTEMPTS) {
                lockoutExpiry = LocalDateTime.now().plusMinutes(COOLDOWN_MINUTES);
            }
            return new AttemptTracker(newAttempts, lockoutExpiry);
        });
    }

    public void loginSucceeded(String identifier) {
        attemptsMap.remove(identifier);
    }

    public boolean isBlocked(String identifier) {
        AttemptTracker tracker = attemptsMap.get(identifier);
        if (tracker == null) {
            return false;
        }
        if (tracker.getLockoutExpiry() != null) {
            if (tracker.getLockoutExpiry().isBefore(LocalDateTime.now())) {
                attemptsMap.remove(identifier);
                return false;
            }
            return true;
        }
        return false;
    }

    public int getRemainingCooldownMinutes(String identifier) {
        AttemptTracker tracker = attemptsMap.get(identifier);
        if (tracker == null || tracker.getLockoutExpiry() == null) {
            return 0;
        }
        long diff = java.time.Duration.between(LocalDateTime.now(), tracker.getLockoutExpiry()).toMinutes();
        return Math.max(1, (int) diff); // round up to at least 1 minute
    }

    private static class AttemptTracker {
        private final int attempts;
        private final LocalDateTime lockoutExpiry;

        public AttemptTracker(int attempts, LocalDateTime lockoutExpiry) {
            this.attempts = attempts;
            this.lockoutExpiry = lockoutExpiry;
        }

        public int getAttempts() {
            return attempts;
        }

        public LocalDateTime getLockoutExpiry() {
            return lockoutExpiry;
        }
    }
}
