package com.ailoganalyzer.loganalyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logAnalyzerOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");

        Contact contact = new Contact()
                .name("Log Analyzer Team")
                .email("support@loganalyzer.com")
                .url("https://github.com/ai-log-analyzer");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("AI Log Analyzer API")
                .version("1.0.0")
                .description("API documentation for AI-Powered Log Analyzer with GraphQL and REST endpoints")
                .contact(contact)
                .license(mitLicense);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
} 