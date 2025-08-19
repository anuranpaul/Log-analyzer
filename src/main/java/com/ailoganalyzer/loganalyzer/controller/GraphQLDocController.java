package com.ailoganalyzer.loganalyzer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api-docs")
@Tag(name = "GraphQL Documentation", description = "Documentation for GraphQL API")
public class GraphQLDocController {

    @Value("classpath:graphql/schema.graphqls")
    private Resource graphqlSchema;

    @Operation(summary = "Get GraphQL schema", description = "Returns the GraphQL schema definition")
    @GetMapping(value = "/graphql/schema", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String getGraphQLSchema() {
        try (Reader reader = new InputStreamReader(graphqlSchema.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Operation(summary = "GraphQL query examples", description = "Shows examples of GraphQL queries")
    @GetMapping("/graphql/examples")
    public String showGraphQLExamples(Model model) {
        Map<String, String> examples = new HashMap<>();

        // Query examples
        examples.put("Get log by ID",
                "query {\n" +
                        "  log(id: \"1\") {\n" +
                        "    id\n" +
                        "    timestamp\n" +
                        "    application\n" +
                        "    message\n" +
                        "    severity\n" +
                        "  }\n" +
                        "}");

        examples.put("Query logs with filtering",
                "query {\n" +
                        "  logs(\n" +
                        "    filter: {\n" +
                        "      applications: [\"backend-service\"],\n" +
                        "      severities: [ERROR, CRITICAL],\n" +
                        "      startTime: \"2023-04-01T00:00:00Z\"\n" +
                        "    },\n" +
                        "    page: {\n" +
                        "      page: 0,\n" +
                        "      size: 10\n" +
                        "    }\n" +
                        "  ) {\n" +
                        "    content {\n" +
                        "      id\n" +
                        "      message\n" +
                        "      severity\n" +
                        "    }\n" +
                        "    totalElements\n" +
                        "  }\n" +
                        "}");

        // Mutation examples
        examples.put("Ingest a new log",
                "mutation {\n" +
                        "  ingestLog(input: {\n" +
                        "    timestamp: \"2024-03-17T14:32:21Z\",\n" +
                        "    application: \"user-service\",\n" +
                        "    message: \"User authentication successful\",\n" +
                        "    severity: INFO,\n" +
                        "    source: \"auth-service\",\n" +
                        "    host: \"prod-server-01\",\n" +
                        "    metadata: [\n" +
                        "      { key: \"user_id\", value: \"12345\" },\n" +
                        "      { key: \"request_id\", value: \"abc-123-xyz\" }\n" +
                        "    ]\n" +
                        "  }) {\n" +
                        "    id\n" +
                        "    timestamp\n" +
                        "    message\n" +
                        "  }\n" +
                        "}");

        // Subscription examples
        examples.put("Subscribe to log alerts",
                "subscription {\n" +
                        "  logAlerts(severity: [ERROR, CRITICAL]) {\n" +
                        "    id\n" +
                        "    timestamp\n" +
                        "    application\n" +
                        "    message\n" +
                        "    severity\n" +
                        "    source\n" +
                        "  }\n" +
                        "}");

        model.addAttribute("examples", examples);
        model.addAttribute("graphiqlUrl", "/graphiql");
        model.addAttribute("subscriptionTestUrl", "/subscription-test");

        return "graphql-examples";
    }

    @Operation(summary = "Custom GraphiQL Interface", description = "Self-contained GraphiQL interface without CORS issues")
    @GetMapping(value = "/graphiql", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String customGraphiQL() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>GraphiQL - AI Log Analyzer</title>
                    <style>
                        html, body {
                            height: 100%;
                            margin: 0;
                            width: 100%;
                            overflow: hidden;
                        }
                        #graphiql {
                            height: 100vh;
                        }
                    </style>
                    <link
                        rel="stylesheet"
                        href="https://cdn.jsdelivr.net/npm/graphiql@3.0.6/graphiql.min.css"
                    />
                </head>
                <body>
                    <div id="graphiql">Loading GraphiQL...</div>
                    <script
                        crossorigin
                        src="https://cdn.jsdelivr.net/npm/react@18/umd/react.development.js"
                    ></script>
                    <script
                        crossorigin
                        src="https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.development.js"
                    ></script>
                    <script
                        crossorigin
                        src="https://cdn.jsdelivr.net/npm/graphiql@3.0.6/graphiql.min.js"
                    ></script>
                    <script>
                        const graphQLFetcher = (graphQLParams) =>
                            fetch('/graphql', {
                                method: 'post',
                                headers: {
                                    Accept: 'application/json',
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify(graphQLParams),
                                credentials: 'same-origin'
                            })
                            .then((response) => {
                                return response.text();
                            })
                            .then((responseBody) => {
                                try {
                                    return JSON.parse(responseBody);
                                } catch (e) {
                                    return responseBody;
                                }
                            });

                        ReactDOM.render(
                            React.createElement(GraphiQL, {
                                fetcher: graphQLFetcher,
                                defaultVariableEditorOpen: true,
                                headerEditorEnabled: true,
                                defaultQuery: `# Welcome to GraphiQL for AI Log Analyzer
                # Type queries here and use Ctrl+Enter to execute
                #
                # Example query:
                query {
                  logs(
                    filter: {
                      applications: ["backend-service"],
                      severities: [ERROR, CRITICAL]
                    },
                    page: {
                      page: 0,
                      size: 10
                    }
                  ) {
                    content {
                      id
                      message
                      severity
                      timestamp
                    }
                    totalElements
                  }
                }
                `
                            }),
                            document.getElementById('graphiql'),
                        );
                    </script>
                </body>
                </html>
                                """;
    }
}