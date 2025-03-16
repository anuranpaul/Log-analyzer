package com.ailoganalyzer.loganalyzer.graphql.datafetcher;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.repository.LogElasticsearchRepository;
import com.ailoganalyzer.loganalyzer.repository.LogRepository;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@DgsComponent
@RequiredArgsConstructor
public class LogDataFetcher {

    private final LogRepository logRepository;
    private final LogElasticsearchRepository logElasticsearchRepository;

    @DgsQuery
    public Optional<Log> log(@InputArgument String id) {
        return logRepository.findById(Long.valueOf(id));
    }

    @DgsQuery
    public Map<String, Object> logs(@InputArgument Map<String, Object> filter, 
                                    @InputArgument Map<String, Object> page) {
        
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