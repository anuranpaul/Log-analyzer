package com.ailoganalyzer.loganalyzer.repository;

import com.ailoganalyzer.loganalyzer.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
} 