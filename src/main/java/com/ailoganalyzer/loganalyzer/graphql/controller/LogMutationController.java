package com.ailoganalyzer.loganalyzer.graphql.controller;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.MetadataField;
import com.ailoganalyzer.loganalyzer.model.Severity;
import com.ailoganalyzer.loganalyzer.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LogMutationController {

    private final LogService logService;

    @MutationMapping
    public Log ingestLog(@Argument Map<String, Object> input) {
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
        
        // Save using the service
        return logService.saveLog(log);
    }
} 