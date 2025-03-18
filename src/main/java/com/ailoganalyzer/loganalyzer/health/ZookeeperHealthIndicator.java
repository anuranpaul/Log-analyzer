package com.ailoganalyzer.loganalyzer.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Component
public class ZookeeperHealthIndicator implements HealthIndicator {

    private final String zookeeperHost;
    private final int zookeeperPort;
    private final KafkaAdmin kafkaAdmin;
    private static final int TIMEOUT_MS = 5000;

    public ZookeeperHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            KafkaAdmin kafkaAdmin) {
        // Typically Zookeeper runs on 2181
        this.zookeeperHost = bootstrapServers.split(":")[0];
        this.zookeeperPort = 2181; // Default Zookeeper port
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        Health.Builder healthBuilder = new Health.Builder();
        
        // First, try a basic socket connection to check if Zookeeper is up
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(zookeeperHost, zookeeperPort), TIMEOUT_MS);
            
            // If socket connection succeeded, do a deeper check using Kafka connection
            // which depends on Zookeeper
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                // If we can get the cluster ID, it means Kafka is connected to Zookeeper
                String clusterId = adminClient.describeCluster().clusterId().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                
                return healthBuilder
                        .up()
                        .withDetail("host", zookeeperHost)
                        .withDetail("port", zookeeperPort)
                        .withDetail("kafka.clusterId", clusterId)
                        .build();
            } catch (Exception e) {
                // Kafka couldn't connect, which may indicate Zookeeper issues
                return healthBuilder
                        .down()
                        .withDetail("host", zookeeperHost)
                        .withDetail("port", zookeeperPort)
                        .withDetail("reason", "Zookeeper reachable but Kafka connection failed: " + e.getMessage())
                        .build();
            }
        } catch (Exception e) {
            // Socket connection failed, Zookeeper is definitely down
            return healthBuilder
                    .down()
                    .withDetail("host", zookeeperHost)
                    .withDetail("port", zookeeperPort)
                    .withDetail("error", "Connection refused: " + e.getMessage())
                    .build();
        }
    }
} 