package com.ailoganalyzer.loganalyzer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.ailoganalyzer.loganalyzer.repository.jpa"
)
public class JpaConfig {
    // Configuration is handled by Spring Boot auto-configuration
} 