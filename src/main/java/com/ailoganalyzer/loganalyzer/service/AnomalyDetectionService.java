
package com.ailoganalyzer.loganalyzer.service;

import com.ailoganalyzer.loganalyzer.model.Anomaly;
import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.repository.elasticsearch.LogElasticsearchRepository;
import com.ailoganalyzer.loganalyzer.service.ai.AiAssistant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final LogElasticsearchRepository logRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final AiAssistant aiAssistant;
    private final KafkaProducerService kafkaProducerService;

    @Async
    public void analyzeRecentLogs() {
        try {
            List<Anomaly> anomalies = detectAnomalies();
            for (Anomaly anomaly : anomalies) {
                processAnomalyWithAi(anomaly);
            }
        } catch (Exception e) {
            log.error("Error during anomaly detection: {}", e.getMessage(), e);
        }
    }

    private List<Anomaly> detectAnomalies() {
        List<Anomaly> anomalies = new ArrayList<>();

        // Check for error rate spikes
        anomalies.addAll(detectErrorSpikes());

        // Check for volume drops
        anomalies.addAll(detectVolumeAnomalies());

        // Check for new error patterns
        anomalies.addAll(detectNewErrorPatterns());

        return anomalies;
    }

    private List<Anomaly> detectErrorSpikes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);

        // Query error logs from last hour
        Criteria recentCriteria = new Criteria("timestamp")
                .between(oneHourAgo.toInstant(ZoneOffset.UTC), now.toInstant(ZoneOffset.UTC))
                .and("severity").in("ERROR", "FATAL");

        SearchHits<Log> recentErrors = elasticsearchOperations.search(
                new CriteriaQuery(recentCriteria), Log.class);

        // Query error logs from previous hour
        Criteria previousCriteria = new Criteria("timestamp")
                .between(twoHoursAgo.toInstant(ZoneOffset.UTC), oneHourAgo.toInstant(ZoneOffset.UTC))
                .and("severity").in("ERROR", "FATAL");

        SearchHits<Log> previousErrors = elasticsearchOperations.search(
                new CriteriaQuery(previousCriteria), Log.class);

        long recentCount = recentErrors.getTotalHits();
        long previousCount = previousErrors.getTotalHits();

        // Detect spike (more than 50% increase and at least 10 errors)
        if (recentCount > previousCount * 1.5 && recentCount >= 10) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("recent_error_count", recentCount);
            metrics.put("previous_error_count", previousCount);
            metrics.put("increase_percentage", ((double) (recentCount - previousCount) / previousCount) * 100);

            return List.of(Anomaly.builder()
                    .id(UUID.randomUUID().toString())
                    .type("ERROR_SPIKE")
                    .detectedAt(now)
                    .severity("HIGH")
                    .description(String.format("Error rate increased from %d to %d in the last hour",
                            previousCount, recentCount))
                    .metrics(metrics)
                    .confidence(0.85)
                    .build());
        }

        return new ArrayList<>();
    }

    private List<Anomaly> detectVolumeAnomalies() {
        // Similar implementation for volume drops
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);

        Criteria recentCriteria = new Criteria("timestamp")
                .between(oneHourAgo.toInstant(ZoneOffset.UTC), now.toInstant(ZoneOffset.UTC));

        SearchHits<Log> recentLogs = elasticsearchOperations.search(
                new CriteriaQuery(recentCriteria), Log.class);

        Criteria previousCriteria = new Criteria("timestamp")
                .between(twoHoursAgo.toInstant(ZoneOffset.UTC), oneHourAgo.toInstant(ZoneOffset.UTC));

        SearchHits<Log> previousLogs = elasticsearchOperations.search(
                new CriteriaQuery(previousCriteria), Log.class);

        long recentCount = recentLogs.getTotalHits();
        long previousCount = previousLogs.getTotalHits();

        // Detect significant drop (more than 70% decrease and previous count > 50)
        if (previousCount > 50 && recentCount < previousCount * 0.3) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("recent_log_count", recentCount);
            metrics.put("previous_log_count", previousCount);
            metrics.put("decrease_percentage", ((double) (previousCount - recentCount) / previousCount) * 100);

            return List.of(Anomaly.builder()
                    .id(UUID.randomUUID().toString())
                    .type("VOLUME_DROP")
                    .detectedAt(now)
                    .severity("MEDIUM")
                    .description(String.format("Log volume dropped from %d to %d in the last hour",
                            previousCount, recentCount))
                    .metrics(metrics)
                    .confidence(0.75)
                    .build());
        }

        return new ArrayList<>();
    }

    private List<Anomaly> detectNewErrorPatterns() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        Criteria criteria = new Criteria("timestamp")
                .between(oneHourAgo.toInstant(ZoneOffset.UTC), now.toInstant(ZoneOffset.UTC))
                .and("severity").in("ERROR", "FATAL");

        SearchHits<Log> errorLogs = elasticsearchOperations.search(
                new CriteriaQuery(criteria), Log.class);

        // Group errors by message pattern
        Map<String, Long> errorPatterns = errorLogs.getSearchHits().stream()
                .map(hit -> hit.getContent().getMessage())
                .collect(Collectors.groupingBy(
                        message -> extractErrorPattern(message),
                        Collectors.counting()));

        List<Anomaly> anomalies = new ArrayList<>();
        for (Map.Entry<String, Long> pattern : errorPatterns.entrySet()) {
            if (pattern.getValue() >= 5) { // New pattern with at least 5 occurrences
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("pattern", pattern.getKey());
                metrics.put("occurrences", pattern.getValue());

                anomalies.add(Anomaly.builder()
                        .id(UUID.randomUUID().toString())
                        .type("NEW_ERROR_PATTERN")
                        .detectedAt(now)
                        .severity("MEDIUM")
                        .description(String.format("New error pattern detected: %s (%d occurrences)",
                                pattern.getKey(), pattern.getValue()))
                        .metrics(metrics)
                        .confidence(0.70)
                        .build());
            }
        }

        return anomalies;
    }

    private String extractErrorPattern(String message) {
        // Simple pattern extraction - remove timestamps, IDs, and numbers
        return message.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "[TIMESTAMP]")
                .replaceAll("\\b\\d+\\b", "[NUMBER]")
                .replaceAll("\\b[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}\\b",
                        "[UUID]")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void processAnomalyWithAi(Anomaly anomaly) {
        try {
            String anomalyData = buildAnomalyPrompt(anomaly);
            String aiExplanation = aiAssistant.analyzeAnomaly(anomalyData);
            String suggestedAction = aiAssistant.suggestRootCause(anomaly.getDescription());

            anomaly.setAiExplanation(aiExplanation);
            anomaly.setSuggestedAction(suggestedAction);

            // Send enhanced anomaly to Kafka for real-time alerts
            kafkaProducerService.sendAnomalyAlert(anomaly);

            log.info("AI-enhanced anomaly processed: {}", anomaly.getId());
        } catch (Exception e) {
            log.error("Error processing anomaly with AI: {}", e.getMessage(), e);
        }
    }

    private String buildAnomalyPrompt(Anomaly anomaly) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Anomaly Type: ").append(anomaly.getType()).append("\n");
        prompt.append("Detected At: ").append(anomaly.getDetectedAt()).append("\n");
        prompt.append("Description: ").append(anomaly.getDescription()).append("\n");
        prompt.append("Severity: ").append(anomaly.getSeverity()).append("\n");
        prompt.append("Confidence: ").append(anomaly.getConfidence()).append("\n");

        if (anomaly.getMetrics() != null) {
            prompt.append("Metrics:\n");
            anomaly.getMetrics()
                    .forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append("\n"));
        }

        prompt.append("\nPlease analyze this anomaly and provide a clear explanation of what might be causing it.");
        return prompt.toString();
    }
}
