# AI-Powered Log Analyzer with GraphQL

This project is a modern log analysis system that allows for efficient storage, retrieval, and real-time monitoring of application logs using a GraphQL API. It leverages AI capabilities for advanced log analysis and pattern detection.

## Technology Stack Overview

### Core Technologies

#### Spring Boot 3
- **Purpose**: Serves as our application framework and provides core infrastructure
- **Usage in our system**:
  - Dependency injection and application configuration
  - RESTful service support (though we primarily use GraphQL)
  - Integration with JPA, Elasticsearch, and Kafka
  - Transaction management and database connections
  - Application lifecycle management

#### Spring GraphQL
- **Purpose**: Provides a flexible, efficient API layer for log data access
- **Usage in our system**:
  - Defining schema-first API design
  - Handling complex log queries with nested filters
  - Supporting real-time log subscriptions (upcoming)
  - Reducing over-fetching and under-fetching of log data
  - Enabling clients to request exactly the log fields they need

### Data Storage & Search

#### PostgreSQL
- **Purpose**: Primary persistent storage for log data
- **Usage in our system**:
  - Storing structured log data with ACID guarantees
  - Maintaining relationships between logs and metadata
  - Providing transactional consistency for log ingestion
  - Supporting complex SQL queries when needed
  - Serving as the source of truth for log data

#### Elasticsearch
- **Purpose**: Advanced search and analytics engine
- **Usage in our system**:
  - Full-text search across log messages
  - Real-time log data indexing
  - Complex queries on log metadata
  - Time-based log analysis
  - Aggregations and analytics on log data
  - Efficient filtering and faceted search

### Message Processing

#### Apache Kafka
- **Purpose**: Reliable log message streaming and processing
- **Usage in our system**:
  - Decoupling log ingestion from processing
  - Ensuring reliable delivery of log messages
  - Enabling async processing of logs
  - Supporting high-throughput log ingestion
  - Providing message persistence and replay capabilities
  - Facilitating future integration with other systems

#### Apache Zookeeper
- **Purpose**: Manages Kafka cluster coordination
- **Usage in our system**:
  - Maintaining Kafka broker configurations
  - Managing topic configurations
  - Handling leader election for Kafka partitions
  - Providing distributed synchronization

### Real-time Communication

#### WebSockets
- **Purpose**: Enable real-time log alerts and monitoring
- **Usage in our system**:
  - Supporting GraphQL subscriptions
  - Real-time log event notifications
  - Live dashboard updates
  - Instant alert delivery for critical logs
  - Filtered log streams by severity and application

## System Architecture

Our log analyzer implements a modern event-driven architecture:

1. **Log Ingestion Flow**:
   - Logs are received through GraphQL mutations
   - Stored in PostgreSQL for persistence
   - Published to Kafka for async processing
   - Consumed and indexed in Elasticsearch
   - Broadcast to WebSocket subscribers in real-time

2. **Query Flow**:
   - Simple queries served directly from PostgreSQL
   - Complex search queries routed to Elasticsearch
   - Real-time alerts delivered via WebSocket subscriptions

3. **Processing Flow**:
   - Kafka ensures reliable message processing
   - Enables future extension for:
     - Log aggregation
     - Pattern detection
     - Anomaly detection
     - Alert generation

This architecture provides:
- High availability
- Scalability
- Real-time processing
- Reliable data persistence
- Flexible search capabilities
- Future extensibility

## Phase 1: Core API Development Progress

### ✅ Step 1: Set up Spring Boot GraphQL Project
- Spring Boot 3 project with GraphQL, Elasticsearch, WebSockets, and PostgreSQL
- Basic GraphQL schema and resolvers
- Implemented GraphQL queries and mutations for log management

### ✅ Step 2: Implement Log Storage (Elasticsearch + PostgreSQL)
- Configured Elasticsearch for log storage and search
- Implemented PostgreSQL for persistent storage
- Defined Log entity and repositories using Spring Data
- Implemented GraphQL queries with advanced filtering capabilities
- Added metadata support for flexible log attributes

### ✅ Step 3: Implement Log Streaming (Kafka)
- Configured Kafka for log message streaming
- Implemented producer service for sending logs to Kafka
- Implemented consumer service for processing logs and storing in Elasticsearch
- Added asynchronous log processing pipeline

### 🔲 Step 4: Implement GraphQL Subscription for Real-time Alerts
- Basic subscription setup completed
- Real-time alert filtering by severity
- WebSocket configuration pending
- Alert rules engine pending

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- Elasticsearch 8.x
- Apache Kafka & Zookeeper

### Database Setup

1. Create the PostgreSQL database:
```bash
psql -U postgres -f database_setup.sql
```

2. Start Elasticsearch:
```bash
# Check Elasticsearch status
curl http://localhost:9200
```

3. Start Zookeeper:
```bash
/opt/homebrew/bin/zkServer start-foreground
```

4. Start Kafka:
```bash
/opt/homebrew/opt/kafka/bin/kafka-server-start /opt/homebrew/etc/kafka/server.properties
```

5. Create Kafka topic:
```bash
/opt/homebrew/opt/kafka/bin/kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic log-events
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
      startTime: "2023-04-01T00:00:00Z",
      metadata: {
        key: "environment",
        value: "production"
      }
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

### Mutations

Ingest a new log:
```graphql
mutation {
  ingestLog(input: {
    timestamp: "2024-03-17T14:32:21Z",
    application: "user-service",
    message: "User authentication successful",
    severity: INFO,
    source: "auth-service",
    host: "prod-server-01",
    metadata: [
      { key: "user_id", value: "12345" },
      { key: "request_id", value: "abc-123-xyz" }
    ]
  }) {
    id
    timestamp
    message
    severity
    metadata {
      key
      value
    }
  }
}
```

### Subscriptions (Coming Soon)

Subscribe to log alerts:
```graphql
subscription {
  logAlerts(severity: [ERROR, CRITICAL]) {
    id
    timestamp
    application
    message
    severity
  }
}
```

### Real-time Log Alerts with Subscriptions

Subscribe to log alerts with optional severity filtering:

```graphql
subscription {
  logAlerts(severity: [ERROR, CRITICAL]) {
    id
    timestamp
    application
    message
    severity
    source
    host
    metadata {
      key
      value
    }
  }
}
```

This subscription will push new logs in real-time whenever they match the specified severity levels.

### Testing WebSocket Subscriptions

A simple test client is available at:

```
http://localhost:8080/subscription-test
```

This page allows you to:
- Subscribe to all logs or filter by severity
- Send test logs with different severity levels
- View real-time log alerts as they arrive

## Health Monitoring

The application includes comprehensive health monitoring for all system components, providing real-time status information to ensure reliability and assist in troubleshooting.

### Health Dashboard

A visual health dashboard is available at:

```
http://localhost:8080/system-health
```

This dashboard displays:
- Overall system health status
- Individual component health status (PostgreSQL, Elasticsearch, Kafka, Zookeeper)
- Real-time updates with automatic refresh

### Health API

Health data is also available programmatically via REST:

```
http://localhost:8080/api/health
```

This endpoint returns a JSON representation of all health indicators, including:
- Overall system status
- Component-specific status
- Detailed health information for each component

Note: You can also access individual health components via the Actuator endpoints:
```
http://localhost:8080/actuator/health/elasticsearch
http://localhost:8080/actuator/health/db
http://localhost:8080/actuator/health/kafka
```

### Health Indicators

The application monitors the following components:
- **Database**: Checks PostgreSQL connectivity and response time
- **Elasticsearch**: Verifies index existence and cluster health
- **Kafka**: Validates broker availability and topics
- **Zookeeper**: Confirms service availability and connection state

These health checks help identify issues early and assist with diagnosing problems during deployment and operation.