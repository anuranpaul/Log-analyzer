package com.ailoganalyzer.loganalyzer.service;

import com.ailoganalyzer.loganalyzer.graphql.subscription.LogSubscription;
import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.repository.elasticsearch.LogElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final LogElasticsearchRepository elasticsearchRepository;
    private final LogSubscription logSubscription;

    @KafkaListener(topics = "${spring.kafka.topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeLog(Log logMessage) {
        try {
            log.info("Received log message: {}", logMessage);
            elasticsearchRepository.save(logMessage);
            log.info("Successfully saved log to Elasticsearch: {}", logMessage.getId());
            
            // Publish the log message to subscribers
            logSubscription.publishLogAlert(logMessage);
            log.info("Published log to subscribers: {}", logMessage.getId());
        } catch (Exception e) {
            log.error("Error processing log message: {}", e.getMessage(), e);
        }
    }
} 