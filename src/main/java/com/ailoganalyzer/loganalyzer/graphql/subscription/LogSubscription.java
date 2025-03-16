package com.ailoganalyzer.loganalyzer.graphql.subscription;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Controller
public class LogSubscription {

    private final Sinks.Many<Log> logSink = Sinks.many().multicast().onBackpressureBuffer();

    @SubscriptionMapping
    public Publisher<Log> logAlerts(@Argument List<Severity> severity) {
        List<Severity> filteredSeverities = severity != null ? severity : Arrays.asList(Severity.values());
        
        return logSink.asFlux()
                .filter(log -> filteredSeverities.contains(log.getSeverity()));
    }
    
    // This method would be called when a new log is ingested and should be published to subscribers
    public void publishLogAlert(Log log) {
        logSink.tryEmitNext(log);
    }
} 