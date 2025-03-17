package com.ailoganalyzer.loganalyzer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.ailoganalyzer.loganalyzer.repository.jpa",
    considerNestedRepositories = true
)
@EnableTransactionManagement
public class JpaConfig {
    // JPA configuration if needed
} 