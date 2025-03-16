package com.ailoganalyzer.loganalyzer.graphql.mutation;

import com.ailoganalyzer.loganalyzer.graphql.subscription.LogSubscription;
import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.MetadataField;
import com.ailoganalyzer.loganalyzer.model.Severity;
import com.ailoganalyzer.loganalyzer.repository.LogElasticsearchRepository;
import com.ailoganalyzer.loganalyzer.repository.LogRepository;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DgsComponent
@RequiredArgsConstructor
public class LogMutation {

    private final LogRepository logRepository;
    private final LogElasticsearchRepository logElasticsearchRepository;
    private final LogSubscription logSubscription;

    @DgsMutation
    public Log ingestLog(@InputArgument Map<String, Object> input) {
        // Convert the input to a Log object
        Log log = new Log();
        
        if (input.containsKey("timestamp")) {
            String timestampStr = (String) input.get("timestamp");
            log.setTimestamp(Instant.parse(timestampStr));
        } else {
            log.setTimestamp(Instant.now());
        }
        
        log.setApplication((String) input.get("application"));
        log.setMessage((String) input.get("message"));
        log.setSeverity(Severity.valueOf((String) input.get("severity")));
        
        if (input.containsKey("source")) {
            log.setSource((String) input.get("source"));
        }
        
        if (input.containsKey("host")) {
            log.setHost((String) input.get("host"));
        }
        
        if (input.containsKey("metadata")) {
            List<Map<String, String>> metadataInput = (List<Map<String, String>>) input.get("metadata");
            List<MetadataField> metadata = new ArrayList<>();
            
            for (Map<String, String> field : metadataInput) {
                MetadataField metadataField = new MetadataField();
                metadataField.setKey(field.get("key"));
                metadataField.setValue(field.get("value"));
                metadata.add(metadataField);
            }
            
            log.setMetadata(metadata);
        }
        
        // Save to both repositories
        Log savedLog = logRepository.save(log);
        logElasticsearchRepository.save(savedLog);
        
        // Publish log alert for subscription
        logSubscription.publishLogAlert(savedLog);
        
        return savedLog;
    }
}