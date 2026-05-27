package in.maheshshelakee.moneymanager.service;

import in.maheshshelakee.moneymanager.entity.AILogEntity;
import in.maheshshelakee.moneymanager.entity.AISettingsEntity;
import in.maheshshelakee.moneymanager.repository.AILogRepository;
import in.maheshshelakee.moneymanager.repository.AISettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAIControlService {

    private final AILogRepository aiLogRepository;
    private final AISettingsRepository aiSettingsRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Simulated metrics for the AI Insights Dashboard
        metrics.put("averageHealthScore", 78.4);
        metrics.put("predictionAccuracyRate", 91.2);
        
        List<Map<String, Object>> lowestScores = List.of(
            Map.of("id", 101, "name", "John Doe", "email", "john@example.com", "score", 45.2),
            Map.of("id", 102, "name", "Jane Smith", "email", "jane@example.com", "score", 48.9),
            Map.of("id", 103, "name", "Bob Wilson", "email", "bob@example.com", "score", 51.0),
            Map.of("id", 104, "name", "Alice Brown", "email", "alice@example.com", "score", 55.4),
            Map.of("id", 105, "name", "Charlie Davis", "email", "charlie@example.com", "score", 58.1)
        );
        metrics.put("lowestScoreUsers", lowestScores);
        
        List<Map<String, Object>> anomalyStats = List.of(
            Map.of("category", "Dining Out", "anomalies", 12),
            Map.of("category", "Shopping", "anomalies", 8),
            Map.of("category", "Entertainment", "anomalies", 5)
        );
        metrics.put("anomalyStats", anomalyStats);
        
        return metrics;
    }

    @Transactional(readOnly = true)
    public AISettingsEntity getSettings() {
        return aiSettingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> AISettingsEntity.builder().build());
    }

    @Transactional
    public AISettingsEntity updateSettings(AISettingsEntity newSettings) {
        AISettingsEntity settings = getSettings();
        settings.setGlobalAiEnabled(newSettings.getGlobalAiEnabled());
        settings.setConfidenceThreshold(newSettings.getConfidenceThreshold());
        settings.setPredictionWindowDays(newSettings.getPredictionWindowDays());
        return aiSettingsRepository.save(settings);
    }

    @Transactional
    public Map<String, Object> reanalyzeData() {
        long startTime = System.currentTimeMillis();
        int status = 200;
        String payload = "Triggered global reanalysis batch job for 30d window.";
        
        // Simulate REST call to Python ML bridge
        try {
            // String pythonMlEndpoint = "http://python-ml-service:8000/api/analyze";
            // ResponseEntity<String> response = restTemplate.postForEntity(pythonMlEndpoint, null, String.class);
            // status = response.getStatusCodeValue();
            
            // Simulating network delay
            Thread.sleep(800);
        } catch (Exception e) {
            log.error("ML Bridge error (simulated): {}", e.getMessage());
            status = 503;
            payload = "Error communicating with ML Bridge: " + e.getMessage();
        }

        long executionTime = System.currentTimeMillis() - startTime;
        
        AILogEntity logRecord = AILogEntity.builder()
                .endpoint("/admin/ai/reanalyze")
                .requestPayload(payload)
                .responseStatus(status)
                .executionTimeMs(executionTime)
                .build();
        aiLogRepository.save(logRecord);

        Map<String, Object> result = new HashMap<>();
        result.put("success", status == 200);
        result.put("message", status == 200 ? "Analysis job queued successfully." : "Failed to queue analysis job.");
        return result;
    }

    @Transactional(readOnly = true)
    public Page<AILogEntity> getLogs(int page, int size) {
        return aiLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }
}
