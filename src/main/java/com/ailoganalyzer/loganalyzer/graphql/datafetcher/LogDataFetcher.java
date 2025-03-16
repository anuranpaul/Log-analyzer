package com.ailoganalyzer.loganalyzer.graphql.datafetcher;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.repository.LogElasticsearchRepository;
import com.ailoganalyzer.loganalyzer.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LogDataFetcher {

    private final LogRepository logRepository;
    private final LogElasticsearchRepository logElasticsearchRepository;

    @QueryMapping
    public Optional<Log> log(@Argument String id) {
        return logRepository.findById(Long.valueOf(id));
    }

    @QueryMapping
    public Map<String, Object> logs(@Argument Map<String, Object> filter, 
                                    @Argument Map<String, Object> page) {
        
        int pageNumber = page != null ? (int) page.getOrDefault("page", 0) : 0;
        int pageSize = page != null ? (int) page.getOrDefault("size", 20) : 20;
        
        Page<Log> logPage = logRepository.findAll(PageRequest.of(pageNumber, pageSize));
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", logPage.getContent());
        result.put("totalElements", logPage.getTotalElements());
        result.put("totalPages", logPage.getTotalPages());
        result.put("pageNumber", logPage.getNumber());
        result.put("pageSize", logPage.getSize());
        
        return result;
    }
} 