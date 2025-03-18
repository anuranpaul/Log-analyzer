package com.ailoganalyzer.loganalyzer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        Health.Builder healthBuilder = new Health.Builder();
        
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return healthBuilder
                        .up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "available")
                        .build();
            } else {
                return healthBuilder
                        .down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "unexpected response")
                        .build();
            }
        } catch (Exception e) {
            return healthBuilder
                    .down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
} 