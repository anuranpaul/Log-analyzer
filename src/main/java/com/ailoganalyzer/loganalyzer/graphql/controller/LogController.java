package com.ailoganalyzer.loganalyzer.graphql.controller;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import com.ailoganalyzer.loganalyzer.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @QueryMapping
    public Optional<Log> log(@Argument String id) {
        return logService.findById(Long.valueOf(id));
    }

    @QueryMapping
    public Map<String, Object> logs(@Argument Map<String, Object> filter, @Argument Map<String, Object> page) {
        int pageNumber = page != null ? (int) page.getOrDefault("page", 0) : 0;
        int pageSize = page != null ? (int) page.getOrDefault("size", 20) : 20;
        
        List<Log> logs;
        long totalElements;
        
        if (filter != null && !filter.isEmpty()) {
            // Use Elasticsearch for filtered queries
            logs = logService.search(filter, pageNumber, pageSize);
            totalElements = logs.size(); // For simplicity, we're not counting total results
        } else {
            // Use JPA for simple pagination
            Page<Log> logPage = logService.findAll(pageNumber, pageSize);
            logs = logPage.getContent();
            totalElements = logPage.getTotalElements();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", logs);
        result.put("totalElements", totalElements);
        result.put("totalPages", (int) Math.ceil((double) totalElements / pageSize));
        result.put("pageNumber", pageNumber);
        result.put("pageSize", pageSize);
        
        return result;
    }
    
    @QueryMapping
    public List<Log> findLogsByApplicationAndSeverity(
            @Argument String application, 
            @Argument Severity severity) {
        return logService.findByApplicationAndSeverity(application, severity);
    }
} 