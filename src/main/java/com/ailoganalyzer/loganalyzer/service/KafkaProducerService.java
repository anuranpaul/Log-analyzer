package com.ailoganalyzer.loganalyzer.service;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Anomaly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Log> kafkaTemplate;
    private final KafkaTemplate<String, Anomaly> anomalyKafkaTemplate;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    @Value("${spring.kafka.topic.anomaly:anomaly-alerts}")
    private String anomalyTopicName;

    public void sendLog(Log logMessage) {
        try {
            log.info("Sending log message to Kafka: {}", logMessage);
            kafkaTemplate.send(topicName, logMessage);
            log.info("Successfully sent log message to Kafka: {}", logMessage.getId());
        } catch (Exception e) {
            log.error("Error sending log message to Kafka: {}", e.getMessage(), e);
        }
    }

    public void sendAnomalyAlert(Anomaly anomaly) {
        try {
            log.info("Sending anomaly alert to Kafka: {}", anomaly);
            anomalyKafkaTemplate.send(anomalyTopicName, anomaly);
            log.info("Successfully sent anomaly alert to Kafka: {}", anomaly.getId());
        } catch (Exception e) {
            log.error("Error sending anomaly alert to Kafka: {}", e.getMessage(), e);
        }
    }
}
