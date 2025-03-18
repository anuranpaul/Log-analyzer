package com.ailoganalyzer.loganalyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GraphQlWebSocketHandler extends TextWebSocketHandler {

    private final WebGraphQlHandler graphQlHandler;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public GraphQlWebSocketHandler(WebGraphQlHandler graphQlHandler) {
        this.graphQlHandler = graphQlHandler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            // Handle GraphQL over WebSocket protocol messages
            if (payload.contains("connection_init")) {
                session.sendMessage(new TextMessage("{\"type\":\"connection_ack\"}"));
                return;
            }

            // Create a proper WebGraphQlRequest
            URI uri = URI.create("/graphql-ws");
            HttpHeaders headers = new HttpHeaders();
            MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
            Map<String, Object> variables = new HashMap<>();
            Map<String, Object> extensions = new HashMap<>();
            String query = extractQuery(payload);
            
            WebGraphQlRequest graphQlRequest = new WebGraphQlRequest(
                uri,
                headers,
                cookies,
                variables,
                extensions,
                query,
                Locale.getDefault()
            );

            graphQlHandler.handleRequest(graphQlRequest)
                    .flatMap(response -> {
                        try {
                            String jsonResponse = serializeResponse(response);
                            session.sendMessage(new TextMessage(jsonResponse));
                            return Mono.empty();
                        } catch (IOException e) {
                            log.error("Error sending WebSocket message", e);
                            return Mono.error(e);
                        }
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    private String extractQuery(String payload) {
        // Simple extraction of the query from the payload
        // In a real implementation, you would use a proper JSON parser
        if (payload.contains("\"query\"")) {
            int start = payload.indexOf("\"query\"") + 9;
            while (start < payload.length() && payload.charAt(start) != '"') start++;
            start++; // Move past the opening quote
            
            int end = start;
            while (end < payload.length() && payload.charAt(end) != '"') end++;
            
            if (end > start) {
                return payload.substring(start, end);
            }
        }
        return "";
    }

    private String serializeResponse(WebGraphQlResponse response) {
        // Convert the response to a JSON string
        // This is a placeholder; use a proper JSON library like Jackson or Gson
        return response.toString();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket connection closed: {}", session.getId());
    }
} 