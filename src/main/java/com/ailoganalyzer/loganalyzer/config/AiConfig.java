package com.ailoganalyzer.loganalyzer.config;

import com.ailoganalyzer.loganalyzer.service.ai.AiAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "OpenAI API key must be provided in application.properties (ai.openai.api-key)");
        }

        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(openAiModel)
                .build();
    }
}
