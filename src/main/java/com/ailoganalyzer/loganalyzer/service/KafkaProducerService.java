package com.ailoganalyzer.loganalyzer.service;

import com.ailoganalyzer.loganalyzer.model.Log;
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

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    public void sendLog(Log logMessage) {
        CompletableFuture<SendResult<String, Log>> future = kafkaTemplate.send(topicName, 
            String.valueOf(logMessage.getId()), logMessage);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Log sent to topic {}: {}", topicName, logMessage);
            } else {
                log.error("Failed to send log to topic {}: {}", topicName, ex.getMessage());
            }
        });
    }
} 