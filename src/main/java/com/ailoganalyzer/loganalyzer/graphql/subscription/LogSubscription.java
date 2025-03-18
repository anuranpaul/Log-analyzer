package com.ailoganalyzer.loganalyzer.graphql.subscription;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.List;

@Controller
@Slf4j
public class LogSubscription {

    // Use directAllOrNothing to ensure all subscribers get the message
    private final Sinks.Many<Log> logSink = Sinks.many().multicast().directAllOrNothing();

    // Renamed method to avoid ambiguous mapping
    @SubscriptionMapping("allLogAlerts")
    public Publisher<Log> allLogAlerts() {
        log.info("New subscription for all log alerts");
        
        return logSink.asFlux()
                .doOnSubscribe(subscription -> log.info("Client subscribed to all log alerts"))
                .doOnCancel(() -> log.info("Client unsubscribed from log alerts"));
    }

    @SubscriptionMapping
    public Publisher<Log> logAlerts(@Argument List<Severity> severity) {
        List<Severity> filteredSeverities = severity != null ? severity : Arrays.asList(Severity.values());
        
        log.info("New subscription for log alerts with severities: {}", filteredSeverities);
        
        return logSink.asFlux()
                .filter(logEntry -> {
                    boolean matches = filteredSeverities.contains(logEntry.getSeverity());
                    log.debug("Filtering log {} with severity {}: {}", 
                              logEntry.getId(), logEntry.getSeverity(), matches ? "MATCH" : "FILTERED");
                    return matches;
                })
                .doOnSubscribe(subscription -> log.info("Client subscribed to log alerts"))
                .doOnCancel(() -> log.info("Client unsubscribed from log alerts"));
    }
    
    /**
     * This method is called when a new log is ingested and should be published to subscribers
     * It's called by the KafkaConsumerService when new logs are processed
     */
    public void publishLogAlert(Log logEntry) {
        log.info("Publishing log alert: {} with severity {}", logEntry.getId(), logEntry.getSeverity());
        
        Sinks.EmitResult result = logSink.tryEmitNext(logEntry);
        
        if (result.isSuccess()) {
            log.info("Successfully published log alert for ID: {}", logEntry.getId());
        } else {
            log.error("Failed to publish log alert: {}", result);
        }
    }
} 