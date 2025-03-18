package com.ailoganalyzer.loganalyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class ApiIndexController {
    
    @Autowired
    private TemplateEngine templateEngine;

    @GetMapping("/index")
    public String apiIndex(Model model) {
        Map<String, String> endpoints = new LinkedHashMap<>();
        
        // REST API endpoints
        endpoints.put("Swagger UI", "/swagger-ui.html");
        endpoints.put("OpenAPI Spec (JSON)", "/api-docs");
        
        // GraphQL endpoints
        endpoints.put("GraphiQL Interface", "/graphiql");
        endpoints.put("GraphQL Examples", "/api-docs/graphql/examples");
        endpoints.put("GraphQL Schema", "/api-docs/graphql/schema");
        
        // Testing endpoints
        endpoints.put("Subscription Test", "/subscription-test");
        endpoints.put("Health Dashboard", "/system-health");
        endpoints.put("Health API", "/api/health");
        
        model.addAttribute("endpoints", endpoints);
        return "api-index";
    }
    
    // Add a simple text endpoint as an alternative
    @GetMapping("/docs")
    @ResponseBody
    public ResponseEntity<String> apiDocs() {
        StringBuilder sb = new StringBuilder();
        sb.append("AI Log Analyzer API Documentation\n\n");
        
        sb.append("REST API:\n");
        sb.append("- Swagger UI: /swagger-ui.html\n");
        sb.append("- OpenAPI Spec: /api-docs\n\n");
        
        sb.append("GraphQL API:\n");
        sb.append("- GraphiQL Interface: /graphiql\n");
        sb.append("- GraphQL Examples: /api-docs/graphql/examples\n");
        sb.append("- GraphQL Schema: /api-docs/graphql/schema\n\n");
        
        sb.append("Testing & Monitoring:\n");
        sb.append("- Subscription Test: /subscription-test\n");
        sb.append("- Health Dashboard: /system-health\n");
        sb.append("- Health API: /api/health\n");
        
        return ResponseEntity.ok(sb.toString());
    }
} 