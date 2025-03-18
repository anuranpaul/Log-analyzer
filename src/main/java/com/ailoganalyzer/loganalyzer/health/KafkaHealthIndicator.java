package com.ailoganalyzer.loganalyzer.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private static final int TIMEOUT_SECONDS = 5;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        Health.Builder healthBuilder = new Health.Builder();
        
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterOptions options = new DescribeClusterOptions()
                    .timeoutMs((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            
            DescribeClusterResult describeClusterResult = adminClient.describeCluster(options);
            String clusterId = describeClusterResult.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            int nodeCount = describeClusterResult.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).size();
            
            return healthBuilder
                    .up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .build();
        } catch (Exception e) {
            return healthBuilder
                    .down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
} 