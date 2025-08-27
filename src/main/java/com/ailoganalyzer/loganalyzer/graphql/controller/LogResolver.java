package com.ailoganalyzer.loganalyzer.graphql.controller;

import com.ailoganalyzer.loganalyzer.dto.LogWithAi;
import com.ailoganalyzer.loganalyzer.graphql.controller.LogDataFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LogResolver {

    private final LogDataFetcher logDataFetcher;

    @QueryMapping
    public List<LogWithAi> getLogsWithAi(@Argument int limit) {
        return logDataFetcher.getLogsWithAi(limit);
    }
}