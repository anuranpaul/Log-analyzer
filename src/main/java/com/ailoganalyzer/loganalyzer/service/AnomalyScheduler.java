package com.ailoganalyzer.loganalyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyScheduler {

    private final AnomalyDetectionService anomalyDetectionService;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void runAnomalyDetection() {
        log.info("Starting scheduled anomaly detection");
        anomalyDetectionService.analyzeRecentLogs();
        log.info("Completed scheduled anomaly detection");
    }
}
