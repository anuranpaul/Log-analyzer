package com.ailoganalyzer.loganalyzer.health;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component("logAnalyzerHealth")
public class AggregateHealthIndicator implements CompositeHealthContributor {

    private final Map<String, HealthContributor> contributors = new HashMap<>();

    public AggregateHealthIndicator(
            DatabaseHealthIndicator databaseHealthIndicator,
            ElasticsearchHealthIndicator elasticsearchHealthIndicator,
            KafkaHealthIndicator kafkaHealthIndicator,
            ZookeeperHealthIndicator zookeeperHealthIndicator) {
        
        contributors.put("database", databaseHealthIndicator);
        contributors.put("elasticsearch", elasticsearchHealthIndicator);
        contributors.put("kafka", kafkaHealthIndicator);
        contributors.put("zookeeper", zookeeperHealthIndicator);
    }

    @Override
    public HealthContributor getContributor(String name) {
        return contributors.get(name);
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return contributors.entrySet().stream()
                .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
                .iterator();
    }
} 