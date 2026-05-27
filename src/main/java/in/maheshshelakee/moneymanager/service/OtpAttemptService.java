package in.maheshshelakee.moneymanager.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OtpAttemptService {

    private static final int MAX_REQUESTS = 5;
    private static final int REQUEST_WINDOW_MINUTES = 15;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int VERIFICATION_COOLDOWN_MINUTES = 15;

    private final ConcurrentHashMap<String, RequestTracker> requestMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, VerificationTracker> verificationMap = new ConcurrentHashMap<>();

    // ─── REQUEST RATE LIMITING ───────────────────────────────────────────────
    public boolean isRequestBlocked(String identifier) {
        RequestTracker tracker = requestMap.get(identifier);
        if (tracker == null) return false;
        
        cleanOldRequests(tracker);
        return tracker.getRequests().size() >= MAX_REQUESTS;
    }

    public int getRemainingRequestCooldownMinutes(String identifier) {
        RequestTracker tracker = requestMap.get(identifier);
        if (tracker == null) return 0;
        
        cleanOldRequests(tracker);
        if (tracker.getRequests().size() < MAX_REQUESTS) return 0;
        
        LocalDateTime oldestRequest = tracker.getRequests().get(0);
        LocalDateTime blockExpiry = oldestRequest.plusMinutes(REQUEST_WINDOW_MINUTES);
        long diff = java.time.Duration.between(LocalDateTime.now(), blockExpiry).toMinutes();
        return Math.max(1, (int) diff);
    }

    public void recordRequest(String identifier) {
        requestMap.compute(identifier, (key, tracker) -> {
            if (tracker == null) {
                tracker = new RequestTracker();
            }
            cleanOldRequests(tracker);
            tracker.getRequests().add(LocalDateTime.now());
            return tracker;
        });
    }

    private void cleanOldRequests(RequestTracker tracker) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(REQUEST_WINDOW_MINUTES);
        tracker.getRequests().removeIf(time -> time.isBefore(cutoff));
    }

    // ─── VERIFICATION RATE LIMITING ──────────────────────────────────────────
    public boolean isVerificationBlocked(String identifier) {
        VerificationTracker tracker = verificationMap.get(identifier);
        if (tracker == null) return false;

        if (tracker.getLockoutExpiry() != null) {
            if (tracker.getLockoutExpiry().isBefore(LocalDateTime.now())) {
                verificationMap.remove(identifier);
                return false;
            }
            return true;
        }
        return false;
    }

    public int getRemainingVerificationCooldownMinutes(String identifier) {
        VerificationTracker tracker = verificationMap.get(identifier);
        if (tracker == null || tracker.getLockoutExpiry() == null) return 0;

        long diff = java.time.Duration.between(LocalDateTime.now(), tracker.getLockoutExpiry()).toMinutes();
        return Math.max(1, (int) diff);
    }

    public void verificationFailed(String identifier) {
        verificationMap.compute(identifier, (key, tracker) -> {
            if (tracker == null) {
                return new VerificationTracker(1, null);
            }
            if (tracker.getLockoutExpiry() != null && tracker.getLockoutExpiry().isAfter(LocalDateTime.now())) {
                return tracker;
            }
            int newAttempts = tracker.getAttempts() + 1;
            LocalDateTime lockoutExpiry = null;
            if (newAttempts >= MAX_VERIFICATION_ATTEMPTS) {
                lockoutExpiry = LocalDateTime.now().plusMinutes(VERIFICATION_COOLDOWN_MINUTES);
            }
            return new VerificationTracker(newAttempts, lockoutExpiry);
        });
    }

    public void verificationSucceeded(String identifier) {
        verificationMap.remove(identifier);
        requestMap.remove(identifier);
    }

    // ─── TRACKERS ────────────────────────────────────────────────────────────
    private static class RequestTracker {
        private final List<LocalDateTime> requests = new CopyOnWriteArrayList<>();

        public List<LocalDateTime> getRequests() {
            return requests;
        }
    }

    private static class VerificationTracker {
        private final int attempts;
        private final LocalDateTime lockoutExpiry;

        public VerificationTracker(int attempts, LocalDateTime lockoutExpiry) {
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
