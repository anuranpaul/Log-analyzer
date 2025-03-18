package com.ailoganalyzer.loganalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebGraphQlHandler webGraphQlHandler;

    public WebSocketConfig(WebGraphQlHandler webGraphQlHandler) {
        this.webGraphQlHandler = webGraphQlHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(customGraphQlWebSocketHandler(), "/graphql-ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler customGraphQlWebSocketHandler() {
        return new GraphQlWebSocketHandler(webGraphQlHandler);
    }
}

// Separate configuration for GraphQL Interceptor
@Configuration
class GraphQLInterceptorConfig {

    @Bean
    public WebSocketGraphQlInterceptor webSocketGraphQlInterceptor() {
        return new WebSocketGraphQlInterceptor() {
            public WebSocketGraphQlRequest intercept(WebSocketGraphQlRequest request) {
                // Add any headers or connection parameters needed for GraphQL subscriptions
                return request;
            }
        };
    }
} 