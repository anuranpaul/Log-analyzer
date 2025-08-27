package com.ailoganalyzer.loganalyzer.graphql.mutation;

import com.ailoganalyzer.loganalyzer.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AnomalyMutation {

    private final AnomalyDetectionService anomalyDetectionService;

    @MutationMapping
    public String triggerAnomalyAnalysis() {
        log.info("Manual anomaly analysis triggered via GraphQL");
        anomalyDetectionService.analyzeRecentLogs();
        return "Anomaly analysis triggered successfully";
    }
}
