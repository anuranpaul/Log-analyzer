package com.ailoganalyzer.loganalyzer.repository.elasticsearch;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository("elasticsearchLogRepository")
public interface LogElasticsearchRepository extends ElasticsearchRepository<Log, Long> {
    
    // Find logs by application
    List<Log> findByApplication(String application);
    
    // Find logs by severity
    List<Log> findBySeverity(Severity severity);
    
    // Find logs by application and severity
    List<Log> findByApplicationAndSeverity(String application, Severity severity);
    
    // Find logs by time range
    List<Log> findByTimestampBetween(Instant startTime, Instant endTime);
    
    // Find logs by source
    List<Log> findBySource(String source);
    
    // Find logs by host
    List<Log> findByHost(String host);
    
    // Find logs containing a specific message text (using a custom query)
    @Query("{\"bool\": {\"must\": [{\"match\": {\"message\": \"?0\"}}]}}")
    List<Log> findByMessageContaining(String messageText);
    
    // Find logs by application and time range with pagination
    Page<Log> findByApplicationAndTimestampBetween(
            String application, 
            Instant startTime, 
            Instant endTime, 
            Pageable pageable);
} 