# AI-Powered Log Analyzer with GraphQL

This project is a modern log analysis system that allows for efficient storage, retrieval, and real-time monitoring of application logs using a GraphQL API. It leverages AI capabilities for advanced log analysis and pattern detection.

## Phase 1: Core API Development Progress

### ✅ Step 1: Set up Spring Boot GraphQL Project
- Spring Boot 3 project with GraphQL, Elasticsearch, WebSockets, and PostgreSQL
- Basic GraphQL schema and resolvers

### ✅ Step 2: Implement Log Storage (Elasticsearch + PostgreSQL)
- Configured Elasticsearch for log storage
- Defined Log entity and repository using Spring Data Elasticsearch
- Implemented GraphQL queries to fetch logs with filtering

### 🔲 Step 3: Implement Log Streaming (Kafka / Fluentd)
- Not started yet

### 🔲 Step 4: Implement GraphQL Subscription for Real-time Alerts
- Not started yet

## Technologies Used

- **Spring Boot 3**: Framework for creating the backend application
- **Spring GraphQL**: Implementation of GraphQL for Java
- **Elasticsearch**: For scalable log storage and search
- **PostgreSQL**: For relational data storage
- **WebSockets**: For real-time communication and subscription support (upcoming)

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- Elasticsearch 7.x (with ML disabled: `xpack.ml.enabled: false`)

### Database Setup

1. Create the PostgreSQL database:
```bash
psql -U postgres -f database_setup.sql
```

2. Make sure Elasticsearch is running:
```bash
# Check Elasticsearch status
curl http://localhost:9200

# If not running, start Elasticsearch
/opt/homebrew/opt/elasticsearch-full/bin/elasticsearch
```

### Running the application

1. Build the project:
```bash
mvn clean install
```

2. Run the application:
```bash
mvn spring-boot:run
```

3. Access GraphiQL at: http://localhost:8080/graphiql

## GraphQL API Examples

### Queries

Fetch logs with filtering:
```graphql
query {
  logs(
    filter: {
      applications: ["backend-service"],
      severities: [ERROR, CRITICAL],
      startTime: "2023-04-01T00:00:00Z"
    },
    page: {
      page: 0,
      size: 10
    }
  ) {
    content {
      id
      timestamp
      application
      message
      severity
      source
      metadata {
        key
        value
      }
    }
    totalElements
    totalPages
  }
}
```

Get a specific log by ID:
```graphql
query {
  log(id: "1") {
    id
    timestamp
    application
    message
    severity
  }
}
```

Find logs by application and severity:
```graphql
query {
  findLogsByApplicationAndSeverity(
    application: "user-service", 
    severity: ERROR
  ) {
    id
    timestamp
    message
  }
}
```

### Mutations

Ingest a new log:
```graphql
mutation {
  ingestLog(input: {
    timestamp: "2023-04-15T14:32:21Z",
    application: "user-service",
    message: "Database connection failed",
    severity: ERROR,
    source: "application-server",
    host: "prod-server-01",
    metadata: [
      { key: "user_id", value: "12345" },
      { key: "request_id", value: "abc-123-xyz" }
    ]
  }) {
    id
    timestamp
    message
  }
}
```