package com.ailoganalyzer.loganalyzer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import java.io.IOException;

@Component
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private final ElasticsearchClient elasticsearchClient;
    private final String indexName;

    public ElasticsearchHealthIndicator(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = "logs"; // The main index name used in your application
    }

    @Override
    public Health health() {
        Health.Builder healthBuilder = new Health.Builder();
        
        try {
            var response = elasticsearchClient.indices().exists(builder -> 
                builder.index(indexName)
            );
            
            if (response.value()) {
                return healthBuilder
                        .up()
                        .withDetail("index", indexName)
                        .withDetail("status", "available")
                        .build();
            } else {
                return healthBuilder
                        .down()
                        .withDetail("index", indexName)
                        .withDetail("status", "index not found")
                        .build();
            }
        } catch (IOException e) {
            return healthBuilder
                    .down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
} 