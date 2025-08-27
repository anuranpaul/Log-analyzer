
package com.ailoganalyzer.loganalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anomaly {
    private String id;
    private String type; // ERROR_SPIKE, LATENCY_INCREASE, VOLUME_DROP, etc.
    private String application;
    private LocalDateTime detectedAt;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String description;
    private String aiExplanation;
    private String suggestedAction;
    private Map<String, Object> metrics;
    private Double confidence;
}
