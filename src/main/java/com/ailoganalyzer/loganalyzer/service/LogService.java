package com.ailoganalyzer.loganalyzer.service;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import com.ailoganalyzer.loganalyzer.repository.elasticsearch.LogElasticsearchRepository;
import com.ailoganalyzer.loganalyzer.repository.jpa.LogJpaRepository;
import com.ailoganalyzer.loganalyzer.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.json.JsonData;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    @Qualifier("logJpaRepository")
    private final LogJpaRepository logRepository;
    
    @Qualifier("logElasticsearchRepository")
    private final LogElasticsearchRepository logElasticsearchRepository;
    
    private final ElasticsearchOperations elasticsearchOperations;
    
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public Log saveLog(Log log) {
        // Save to PostgreSQL first to get the ID
        Log savedLog = logRepository.save(log);
        
        // Send to Kafka for async processing
        kafkaProducerService.sendLog(savedLog);
        
        return savedLog;
    }

    public Optional<Log> findById(Long id) {
        return logRepository.findById(id);
    }

    public Page<Log> findAll(int page, int size) {
        return logRepository.findAll(PageRequest.of(page, size));
    }

    public List<Log> search(Map<String, Object> filter, int page, int size) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder();
        queryBuilder.withPageable(PageRequest.of(page, size));

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (filter != null) {
            // Filter by applications
            if (filter.containsKey("applications")) {
                List<String> applications = (List<String>) filter.get("applications");
                if (!applications.isEmpty()) {
                    List<Query> queries = new ArrayList<>();
                    for (String app : applications) {
                        TermQuery termQuery = new TermQuery.Builder()
                            .field("application")
                            .value(app)
                            .build();
                        queries.add(new Query.Builder().term(termQuery).build());
                    }
                    boolQueryBuilder.must(queries);
                }
            }

            // Filter by severity
            if (filter.containsKey("severities")) {
                List<String> severities = (List<String>) filter.get("severities");
                if (!severities.isEmpty()) {
                    List<Query> queries = new ArrayList<>();
                    for (String sev : severities) {
                        TermQuery termQuery = new TermQuery.Builder()
                            .field("severity")
                            .value(sev)
                            .build();
                        queries.add(new Query.Builder().term(termQuery).build());
                    }
                    boolQueryBuilder.must(queries);
                }
            }

            // Filter by time range
            // if (filter.containsKey("startTime") || filter.containsKey("endTime")) {
            //     RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();

            //     if (filter.containsKey("startTime")) {
            //         String startTimeStr = (String) filter.get("startTime");
            //         Instant startTime = Instant.parse(startTimeStr);
            //         rangeQueryBuilder.gte(JsonData.of(startTime.toString()));
            //     }

            //     if (filter.containsKey("endTime")) {
            //         String endTimeStr = (String) filter.get("endTime");
            //         Instant endTime = Instant.parse(endTimeStr);
            //         rangeQueryBuilder.lte(JsonData.of(endTime.toString()));
            //     }

            //     // Specify the field directly in the RangeQuery
            //     RangeQuery rangeQuery = rangeQueryBuilder
            //             .field("timestamp")
            //             .build();

            //     Query query = new Query.Builder()
            //             .range(rangeQuery)
            //             .build();

            //     boolQueryBuilder.must(query);
            // }

            // Filter by message text
            if (filter.containsKey("messageContains")) {
                String messageContains = (String) filter.get("messageContains");
                if (!messageContains.isBlank()) {
                    MatchQuery matchQuery = new MatchQuery.Builder()
                        .field("message")
                        .query(messageContains)
                        .build();
                    boolQueryBuilder.must(new Query.Builder().match(matchQuery).build());
                }
            }

            // Filter by metadata
            if (filter.containsKey("metadata")) {
                Map<String, String> metadataFilter = (Map<String, String>) filter.get("metadata");
                if (metadataFilter != null && metadataFilter.containsKey("key") && metadataFilter.containsKey("value")) {
                    String key = metadataFilter.get("key");
                    String value = metadataFilter.get("value");
                    
                    // Use the direct repository method instead of building a complex query
                    List<Log> logsWithMetadata = logElasticsearchRepository.findByMetadataKeyAndValue(key, value);
                    return logsWithMetadata;
                }
            }

            // Filter by sources
            if (filter.containsKey("sources")) {
                List<String> sources = (List<String>) filter.get("sources");
                if (!sources.isEmpty()) {
                    List<Query> queries = new ArrayList<>();
                    for (String src : sources) {
                        TermQuery termQuery = new TermQuery.Builder()
                            .field("source")
                            .value(src)
                            .build();
                        queries.add(new Query.Builder().term(termQuery).build());
                    }
                    boolQueryBuilder.must(queries);
                }
            }

            // Filter by hosts
            if (filter.containsKey("hosts")) {
                List<String> hosts = (List<String>) filter.get("hosts");
                if (!hosts.isEmpty()) {
                    List<Query> queries = new ArrayList<>();
                    for (String host : hosts) {
                        TermQuery termQuery = new TermQuery.Builder()
                            .field("host")
                            .value(host)
                            .build();
                        queries.add(new Query.Builder().term(termQuery).build());
                    }
                    boolQueryBuilder.must(queries);
                }
            }
        }

        BoolQuery boolQuery = boolQueryBuilder.build();
        Query query = new Query.Builder().bool(boolQuery).build();

        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(query)
            .withPageable(PageRequest.of(page, size))
            .build();

        SearchHits<Log> searchHits = elasticsearchOperations.search(nativeQuery, Log.class);

        return searchHits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }

    public long countAll() {
        return logRepository.count();
    }

    public List<Log> findByApplicationAndSeverity(String application, Severity severity) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder();
        
        MatchQuery applicationMatchQuery = new MatchQuery.Builder()
            .field("application")
            .query(application)
            .build();
        
        MatchQuery severityMatchQuery = new MatchQuery.Builder()
            .field("severity")
            .query(severity.name())
            .build();
        
        Query applicationQuery = new Query.Builder().match(applicationMatchQuery).build();
        Query severityQuery = new Query.Builder().match(severityMatchQuery).build();
        
        List<Query> mustQueries = new ArrayList<>();
        mustQueries.add(applicationQuery);
        mustQueries.add(severityQuery);
        
        BoolQuery boolQuery = new BoolQuery.Builder()
            .must(mustQueries)
            .build();
        
        Query query = new Query.Builder().bool(boolQuery).build();
        
        NativeQuery searchQuery = queryBuilder
            .withQuery(query)
            .build();

        SearchHits<Log> searchHits = elasticsearchOperations.search(searchQuery, Log.class);
        
        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    public List<Log> findByMetadata(String key, String value, int page, int size) {
        return logElasticsearchRepository.findByMetadataKeyAndValue(key, value, PageRequest.of(page, size)).getContent();
    }
}