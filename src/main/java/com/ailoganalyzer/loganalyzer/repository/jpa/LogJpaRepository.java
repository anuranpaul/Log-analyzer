package com.ailoganalyzer.loganalyzer.repository.jpa;

import com.ailoganalyzer.loganalyzer.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("logJpaRepository")
public interface LogJpaRepository extends JpaRepository<Log, Long> {
    // Basic CRUD operations are provided by JpaRepository
    // Add any specific JPA queries if needed
} 