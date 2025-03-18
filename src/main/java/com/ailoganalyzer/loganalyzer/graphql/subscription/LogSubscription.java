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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@Slf4j
public class LogSubscription {

    // Use a replay sink with history to ensure late subscribers can get messages
    // This is better for handling subscriptions that might connect/disconnect
    private final Sinks.Many<Log> logSink = Sinks.many().replay().limit(100);
    
    // Keep track of active subscribers
    private final AtomicInteger subscriberCount = new AtomicInteger(0);

    // Add a warmup log to ensure the sink is initialized
    {
        Log warmupLog = new Log();
        warmupLog.setId(0L); // Use a Long value for ID
        warmupLog.setMessage("Subscription system initialized");
        warmupLog.setSeverity(Severity.INFO);
        warmupLog.setApplication("system");
        warmupLog.setTimestamp(Instant.now()); // Use Instant instead of String
        logSink.tryEmitNext(warmupLog);
        log.info("Subscription system initialized with warmup message");
    }

    // Renamed method to avoid ambiguous mapping
    @SubscriptionMapping("allLogAlerts")
    public Publisher<Log> allLogAlerts() {
        log.info("New subscription for all log alerts");
        
        return logSink.asFlux()
                .filter(log -> log.getId() != 0L)  // Filter out warmup messages
                .doOnSubscribe(subscription -> {
                    subscriberCount.incrementAndGet();
                    log.info("Client subscribed to all log alerts. Active subscribers: {}", subscriberCount.get());
                })
                .doOnCancel(() -> {
                    subscriberCount.decrementAndGet();
                    log.info("Client unsubscribed from log alerts. Active subscribers: {}", subscriberCount.get());
                });
    }

    @SubscriptionMapping
    public Publisher<Log> logAlerts(@Argument List<Severity> severity) {
        List<Severity> filteredSeverities = severity != null ? severity : Arrays.asList(Severity.values());
        
        log.info("New subscription for log alerts with severities: {}", filteredSeverities);
        
        return logSink.asFlux()
                .filter(log -> log.getId() != 0L)  // Filter out warmup messages
                .filter(logEntry -> {
                    boolean matches = filteredSeverities.contains(logEntry.getSeverity());
                    log.debug("Filtering log {} with severity {}: {}", 
                              logEntry.getId(), logEntry.getSeverity(), matches ? "MATCH" : "FILTERED");
                    return matches;
                })
                .doOnSubscribe(subscription -> {
                    subscriberCount.incrementAndGet();
                    log.info("Client subscribed to log alerts with filter. Active subscribers: {}", subscriberCount.get());
                })
                .doOnCancel(() -> {
                    subscriberCount.decrementAndGet();
                    log.info("Client unsubscribed from log alerts. Active subscribers: {}", subscriberCount.get());
                });
    }
    
    /**
     * This method is called when a new log is ingested and should be published to subscribers
     * It's called by the KafkaConsumerService when new logs are processed
     */
    public void publishLogAlert(Log logEntry) {
        log.info("Publishing log alert: {} with severity {}. Active subscribers: {}", 
                logEntry.getId(), logEntry.getSeverity(), subscriberCount.get());
        
        // Use tryEmitNext which returns an EmitResult
        Sinks.EmitResult result = logSink.tryEmitNext(logEntry);
        
        if (result.isSuccess()) {
            log.info("Successfully published log alert for ID: {}", logEntry.getId());
        } else {
            log.error("Failed to publish log alert: {}. Will retry once.", result);
            // Use FAIL_FAST again for the retry since FAIL_SILENT doesn't exist
            logSink.tryEmitNext(logEntry);
        }
    }
} 