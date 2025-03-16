package com.ailoganalyzer.loganalyzer.graphql.subscription;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsSubscription;
import com.netflix.graphql.dgs.InputArgument;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@DgsComponent
public class LogSubscription {

    private final Sinks.Many<Log> logSink = Sinks.many().multicast().onBackpressureBuffer();

    @DgsSubscription
    public Publisher<Log> logAlerts(@InputArgument List<Severity> severity) {
        List<Severity> filteredSeverities = severity != null ? severity : Arrays.asList(Severity.values());
        
        return logSink.asFlux()
                .filter(log -> filteredSeverities.contains(log.getSeverity()));
    }
    
    // This method would be called when a new log is ingested and should be published to subscribers
    public void publishLogAlert(Log log) {
        logSink.tryEmitNext(log);
    }
} 