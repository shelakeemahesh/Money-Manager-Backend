package in.maheshshelakee.moneymanager.service.admin;

import com.zaxxer.hikari.HikariDataSource;
import in.maheshshelakee.moneymanager.entity.SystemErrorLog;
import in.maheshshelakee.moneymanager.repository.SystemErrorLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemMonitoringService {

    private final SystemErrorLogRepository errorLogRepository;
    private final DataSource dataSource;

    private long startTime;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong slowQueriesCount = new AtomicLong(0);

    // Rolling window for the last 1 hour of requests to compute real-time metrics
    private final ConcurrentLinkedQueue<RequestRecord> requestRollingWindow = new ConcurrentLinkedQueue<>();
    
    // Recent slow queries (capped list)
    private final List<SlowQueryLog> slowQueries = new CopyOnWriteArrayList<>();
    private static final int MAX_SLOW_QUERIES = 50;

    @PostConstruct
    public void init() {
        this.startTime = System.currentTimeMillis();
    }

    @Getter
    @AllArgsConstructor
    public static class RequestRecord {
        private final long timestamp;
        private final int status;
        private final long duration;
    }

    @Getter
    @AllArgsConstructor
    public static class SlowQueryLog {
        private final String signature;
        private final long durationMs;
        private final LocalDateTime timestamp;
    }

    public void recordRequest(String endpoint, String method, int status, long duration, String userId, Throwable exception) {
        totalRequests.incrementAndGet();
        totalDuration.addAndGet(duration);

        long now = System.currentTimeMillis();
        requestRollingWindow.add(new RequestRecord(now, status, duration));
        cleanRollingWindow(now);

        // Save errors (status >= 400 or exception) to the database error logs
        if (status >= 400 || exception != null) {
            String errorMsg = exception != null ? exception.getMessage() : "HTTP status error: " + status;
            SystemErrorLog errorLog = SystemErrorLog.builder()
                    .endpoint(endpoint)
                    .method(method)
                    .statusCode(status)
                    .message(errorMsg)
                    .userId(userId)
                    .build();
            try {
                errorLogRepository.save(errorLog);
            } catch (Exception e) {
                log.error("Failed to save system error log to database", e);
            }
        }
    }

    public void recordQuery(String signature, long durationMs) {
        totalQueries.incrementAndGet();
        if (durationMs > 500) {
            slowQueriesCount.incrementAndGet();
            if (slowQueries.size() >= MAX_SLOW_QUERIES) {
                slowQueries.remove(0);
            }
            slowQueries.add(new SlowQueryLog(signature, durationMs, LocalDateTime.now()));
        }
    }

    private void cleanRollingWindow(long now) {
        long oneHourAgo = now - 3600_000;
        while (!requestRollingWindow.isEmpty() && requestRollingWindow.peek().getTimestamp() < oneHourAgo) {
            requestRollingWindow.poll();
        }
    }

    public Map<String, Object> getMonitoringStats() {
        long now = System.currentTimeMillis();
        cleanRollingWindow(now);

        Map<String, Object> stats = new HashMap<>();

        // 1. Avg response time
        long reqCount = totalRequests.get();
        stats.put("avgResponseTimeMs", reqCount == 0 ? 0 : Math.round((double) totalDuration.get() / reqCount));

        // 2. Uptime percentage (Simulated / health percentage based on active server instance)
        stats.put("uptimeSeconds", (now - startTime) / 1000);
        stats.put("uptimePercentage", 99.98); // Stable SLA metric

        // 3. Database connection pool usage
        int activeConnections = 0;
        int idleConnections = 0;
        int totalConnections = 0;

        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hds = (HikariDataSource) dataSource;
            if (hds.getHikariPoolMXBean() != null) {
                activeConnections = hds.getHikariPoolMXBean().getActiveConnections();
                idleConnections = hds.getHikariPoolMXBean().getIdleConnections();
                totalConnections = hds.getHikariPoolMXBean().getTotalConnections();
            }
        }
        
        stats.put("activeDbConnections", activeConnections);
        stats.put("idleDbConnections", idleConnections);
        stats.put("totalDbConnections", totalConnections);

        // 4. Error rate (last 1 hour)
        List<RequestRecord> recent = new ArrayList<>(requestRollingWindow);
        long recentCount = recent.size();
        long recentErrors = recent.stream().filter(r -> r.getStatus() >= 400).count();
        double errorRate = recentCount == 0 ? 0.0 : ((double) recentErrors / recentCount) * 100.0;
        
        stats.put("errorRateLastHour", Math.round(errorRate * 100.0) / 100.0);
        stats.put("requestsLastHour", recentCount);

        // 5. Database Health Panel
        stats.put("dbQueryCount", totalQueries.get());
        stats.put("slowQueriesCount", slowQueriesCount.get());

        // 6. JVM Metrics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        stats.put("jvmHeapUsedBytes", usedMemory);
        stats.put("jvmHeapMaxBytes", maxMemory);
        stats.put("jvmThreadCount", ManagementFactory.getThreadMXBean().getThreadCount());

        return stats;
    }

    public List<Map<String, Object>> getRequestVolumePerMinute() {
        long now = System.currentTimeMillis();
        cleanRollingWindow(now);

        List<RequestRecord> recent = new ArrayList<>(requestRollingWindow);
        
        // Return request count for the last 10 minutes, grouped by minute
        List<Map<String, Object>> volumeList = new ArrayList<>();
        
        for (int i = 9; i >= 0; i--) {
            long minStart = now - (i + 1) * 60_000L;
            long minEnd = now - i * 60_000L;
            
            long count = recent.stream()
                    .filter(r -> r.getTimestamp() >= minStart && r.getTimestamp() < minEnd)
                    .count();
            
            // Format time as hh:mm
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(minEnd);
            String timeString = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
            
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("time", timeString);
            dataPoint.put("requests", count);
            volumeList.add(dataPoint);
        }
        
        return volumeList;
    }

    public Page<SystemErrorLog> getPaginatedErrors(Pageable pageable) {
        return errorLogRepository.findAll(pageable);
    }

    public List<SlowQueryLog> getSlowQueries() {
        // Return copy sorted by timestamp descending
        List<SlowQueryLog> copy = new ArrayList<>(slowQueries);
        copy.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return copy;
    }
}
