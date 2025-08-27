package com.ailoganalyzer.loganalyzer.service.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;


@AiService
public interface AiAssistant {

    @SystemMessage("You are an expert log analyst and DevOps engineer. " +
            "You analyze log patterns, anomalies, and system metrics to provide " +
            "clear explanations and actionable recommendations. " +
            "Always provide concise, practical advice.")
    String analyzeAnomaly(@UserMessage String anomalyData);

    @SystemMessage("You are a technical expert who explains system issues in simple terms. " +
            "Explain the root cause and provide step-by-step troubleshooting recommendations.")
    String suggestRootCause(@UserMessage String problemDescription);

    @SystemMessage("You are a log analysis expert. Summarize the key insights from log data " +
            "and highlight any concerning patterns or trends.")
    String summarizeLogs(@UserMessage String logSummary);
}