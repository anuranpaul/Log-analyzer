package com.ailoganalyzer.loganalyzer.service;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import com.ailoganalyzer.loganalyzer.repository.LogElasticsearchRepository;
import com.ailoganalyzer.loganalyzer.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final LogElasticsearchRepository logElasticsearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional
    public Log saveLog(Log log) {
        // Save to PostgreSQL first to get the ID
        Log savedLog = logRepository.save(log);
        
        // Then save to Elasticsearch
        logElasticsearchRepository.save(savedLog);
        
        return savedLog;
    }

    public Optional<Log> findById(Long id) {
        return logRepository.findById(id);
    }

    public Page<Log> findAll(int page, int size) {
        return logRepository.findAll(PageRequest.of(page, size));
    }

    public List<Log> search(Map<String, Object> filter, int page, int size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Apply filters if provided
        if (filter != null) {
            // Filter by applications
            if (filter.containsKey("applications") && filter.get("applications") != null) {
                List<String> applications = (List<String>) filter.get("applications");
                if (!applications.isEmpty()) {
                    boolQuery.must(QueryBuilders.termsQuery("application", applications));
                }
            }

            // Filter by severity
            if (filter.containsKey("severities") && filter.get("severities") != null) {
                List<String> severities = (List<String>) filter.get("severities");
                if (!severities.isEmpty()) {
                    boolQuery.must(QueryBuilders.termsQuery("severity", severities));
                }
            }

            // Filter by time range
            if (filter.containsKey("startTime") && filter.get("startTime") != null) {
                String startTimeStr = (String) filter.get("startTime");
                Instant startTime = Instant.parse(startTimeStr);
                boolQuery.must(QueryBuilders.rangeQuery("timestamp").gte(startTime.toString()));
            }

            if (filter.containsKey("endTime") && filter.get("endTime") != null) {
                String endTimeStr = (String) filter.get("endTime");
                Instant endTime = Instant.parse(endTimeStr);
                boolQuery.must(QueryBuilders.rangeQuery("timestamp").lte(endTime.toString()));
            }

            // Filter by message text
            if (filter.containsKey("messageContains") && filter.get("messageContains") != null) {
                String messageContains = (String) filter.get("messageContains");
                if (!messageContains.isBlank()) {
                    boolQuery.must(QueryBuilders.matchQuery("message", messageContains));
                }
            }

            // Filter by sources
            if (filter.containsKey("sources") && filter.get("sources") != null) {
                List<String> sources = (List<String>) filter.get("sources");
                if (!sources.isEmpty()) {
                    boolQuery.must(QueryBuilders.termsQuery("source", sources));
                }
            }

            // Filter by hosts
            if (filter.containsKey("hosts") && filter.get("hosts") != null) {
                List<String> hosts = (List<String>) filter.get("hosts");
                if (!hosts.isEmpty()) {
                    boolQuery.must(QueryBuilders.termsQuery("host", hosts));
                }
            }
        }

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<Log> searchHits = elasticsearchOperations.search(searchQuery, Log.class);
        
        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public long countAll() {
        return logRepository.count();
    }

    public List<Log> findByApplicationAndSeverity(String application, Severity severity) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("application", application))
                .must(QueryBuilders.termQuery("severity", severity.name()));

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .build();

        SearchHits<Log> searchHits = elasticsearchOperations.search(searchQuery, Log.class);
        
        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
} 