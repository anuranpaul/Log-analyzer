package com.ailoganalyzer.loganalyzer.graphql.controller;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.dto.LogWithAi;
import com.ailoganalyzer.loganalyzer.dto.AiResponse;
import com.ailoganalyzer.loganalyzer.repository.jpa.LogJpaRepository;
import com.ailoganalyzer.loganalyzer.service.ai.AiAssistant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LogDataFetcher {

        private final LogJpaRepository logJpaRepository;
        private final AiAssistant aiAssistant;

        public List<LogWithAi> getLogsWithAi(int limit) {
                return logJpaRepository.findAll(PageRequest.of(0, limit))
                                .stream()
                                .map(log -> {
                                        // Single AI analysis call
                                        String aiAnalysis = aiAssistant.analyzeAnomaly(log.getMessage());
                                        AiResponse aiResponse = new AiResponse(aiAnalysis, 0.85f);

                                        return new LogWithAi(
                                                        log.getId(),
                                                        log.getTimestamp(),
                                                        log.getApplication(),
                                                        log.getMessage(),
                                                        log.getSeverity() != null ? log.getSeverity().name() : null,
                                                        log.getSource(),
                                                        log.getHost(),
                                                        log.getMetadata(),
                                                        aiResponse);
                                })
                                .collect(Collectors.toList());
        }
}