# Testing Guide for AI Log Analyzer

This document provides step-by-step instructions for testing all implemented features of the AI Log Analyzer.

## Prerequisites

Ensure all services are running:

1. PostgreSQL
2. Elasticsearch
3. Zookeeper
4. Kafka
5. Spring Boot application

## 1. Basic Log Operations

### 1.1 Create a Log Entry

```graphql
mutation {
  ingestLog(input: {
    timestamp: "2024-03-17T14:32:21Z",
    application: "test-app",
    message: "Test log entry",
    severity: INFO,
    source: "test-source",
    host: "localhost",
    metadata: [
      { key: "test_type", value: "basic_operation" },
      { key: "version", value: "1.0" }
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

Expected result:
- Returns the created log with an assigned ID
- Log is stored in PostgreSQL
- Log is sent to Kafka topic
- Log is indexed in Elasticsearch

### 1.2 Retrieve a Log by ID

```graphql
query {
  log(id: "1") {
    id
    timestamp
    application
    message
    severity
    metadata {
      key
      value
    }
  }
}
```

## 2. Advanced Querying

### 2.1 Filter by Application and Severity

```graphql
query {
  logs(
    filter: {
      applications: ["test-app"],
      severities: [INFO, ERROR]
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
    }
    totalElements
  }
}
```

### 2.2 Search by Time Range

```graphql
query {
  logs(
    filter: {
      startTime: "2024-03-17T00:00:00Z",
      endTime: "2024-03-17T23:59:59Z"
    },
    page: {
      page: 0,
      size: 10
    }
  ) {
    content {
      id
      timestamp
      message
    }
    totalElements
  }
}
```

### 2.3 Search by Metadata

```graphql
query {
  logs(
    filter: {
      metadata: {
        key: "test_type",
        value: "basic_operation"
      }
    },
    page: {
      page: 0,
      size: 10
    }
  ) {
    content {
      id
      message
      metadata {
        key
        value
      }
    }
    totalElements
  }
}
```

## 3. Kafka Integration Testing

### 3.1 Verify Log Processing Pipeline

1. Create a log entry using the mutation from section 1.1
2. Check Kafka topic for the message:
```bash
/opt/homebrew/opt/kafka/bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic log-events --from-beginning
```
3. Verify the log appears in Elasticsearch:
```bash
curl -X GET "localhost:9200/logs/_search?q=metadata.test_type:basic_operation"
```

### 3.2 Test Error Handling

Create a log with invalid data to test error handling:
```graphql
mutation {
  ingestLog(input: {
    timestamp: "invalid-timestamp",
    application: "test-app",
    message: "Test error handling",
    severity: INFO
  }) {
    id
    message
  }
}
```

Expected result:
- GraphQL should return a validation error
- No entry in PostgreSQL
- No message in Kafka topic

## 4. Performance Testing

### 4.1 Batch Log Ingestion

Use this script to test ingesting multiple logs:
```bash
for i in {1..100}; do
  curl -X POST -H "Content-Type: application/json" -d '{
    "query": "mutation { ingestLog(input: { timestamp: \"2024-03-17T14:32:21Z\", application: \"perf-test\", message: \"Performance test log $i\", severity: INFO, metadata: [{ key: \"test_type\", value: \"performance\" }] }) { id } }"
  }' http://localhost:8080/graphql
done
```

### 4.2 Query Performance

Test query performance with different page sizes:
```graphql
query {
  logs(
    filter: {
      applications: ["perf-test"]
    },
    page: {
      page: 0,
      size: 1000
    }
  ) {
    content {
      id
      message
    }
    totalElements
  }
}
```

## 5. WebSocket Subscription Testing

### 5.1 Test WebSocket Subscriptions Using the Test Client

1. Access the WebSocket test client:
   ```
   http://localhost:8080/subscription-test
   ```

2. Test connection:
   - The page should automatically connect to the WebSocket endpoint
   - The status should change to "Connected" after a moment
   - If it doesn't connect, check the logs for any errors

### 5.2 Subscribe to Log Alerts

1. Click the "All Logs" button to subscribe to all logs regardless of severity
   - The status should update to "Subscribed to all logs"

2. Test severity filtering by clicking on one of the severity buttons:
   - "INFO Only" - Subscribes to only INFO logs
   - "WARNING Only" - Subscribes to only WARNING logs
   - "ERROR Only" - Subscribes to only ERROR logs
   - "CRITICAL Only" - Subscribes to only CRITICAL logs

3. Verify subscription behavior:
   - Clicking "Unsubscribe" should cancel any active subscriptions
   - The status should update to reflect the current subscription state

### 5.3 Testing with Real-time Log Generation

1. Generate test logs by clicking the severity buttons in the "Send Test Log" section:
   - Each button sends a test log with the corresponding severity level
   - The log should appear immediately in the log display area if subscribed to that severity

2. Verify filtering:
   - When subscribed to a specific severity, only logs of that severity should appear
   - When subscribed to "All Logs", logs of any severity should appear

3. Send a log via GraphQL and check real-time delivery:
   ```graphql
   mutation {
     ingestLog(input: {
       timestamp: "2024-03-17T15:30:00Z",
       application: "websocket-test-app",
       message: "Testing real-time log alerts",
       severity: CRITICAL,
       source: "testing-guide",
       host: "test-client",
       metadata: [
         { key: "test_type", value: "websocket" },
         { key: "realtime", value: "true" }
       ]
     }) {
       id
     }
   }
   ```

   - If subscribed to CRITICAL logs, this log should appear immediately in the WebSocket client

### 5.4 Verify End-to-End Pipeline

1. Send a log via the GraphQL mutation
2. Verify the log is:
   - Saved to PostgreSQL (check via GraphQL query)
   - Processed by Kafka (check consumer groups)
   - Indexed in Elasticsearch
   - Delivered to WebSocket subscribers in real-time

3. Test disconnection and reconnection:
   - Close the test client page and reopen it
   - Verify it reconnects and can resume subscriptions
   - Send a new log and verify it's received after reconnection

## 6. Service Restart Testing

### 6.1 Pre-Restart Data Population

1. Add test logs with different timestamps:
```graphql
mutation {
  ingestLog(input: {
    timestamp: "2024-03-17T12:00:00Z",
    application: "restart-test",
    message: "Pre-restart test log",
    severity: INFO,
    metadata: [
      { key: "test_type", value: "restart" },
      { key: "phase", value: "pre-restart" }
    ]
  }) {
    id
  }
}
```

2. Verify data is stored:
```graphql
query {
  logs(
    filter: {
      applications: ["restart-test"]
    }
  ) {
    content {
      id
      message
      metadata {
        key
        value
      }
    }
  }
}
```

### 6.2 Service Shutdown Testing

Follow this sequence and verify each step:

1. **Spring Boot Application**:
   - Stop with Ctrl+C
   - Verify app logs show clean shutdown
   - Check for any error messages

2. **Kafka**:
   ```bash
   # Stop Kafka
   /opt/homebrew/bin/kafka-server-stop
   
   # Verify Kafka is stopped
   ps aux | grep kafka
   ```

3. **Zookeeper**:
   ```bash
   # Stop Zookeeper
   /opt/homebrew/bin/zkServer stop
   
   # Verify Zookeeper is stopped
   ps aux | grep zookeeper
   ```

4. **Elasticsearch**:
   ```bash
   # Stop Elasticsearch
   brew services stop elasticsearch
   
   # Verify Elasticsearch is stopped
   curl -X GET "localhost:9200/_cluster/health"
   # Should fail to connect
   ```

### 6.3 Service Startup Testing

1. **Start Elasticsearch**:
   ```bash
   brew services start elasticsearch
   
   # Wait 15 seconds, then verify
   curl -X GET "localhost:9200/_cluster/health"
   # Should show "status": "green"
   ```

2. **Start Zookeeper**:
   ```bash
   # Clear data if needed
   rm -rf /opt/homebrew/var/run/zookeeper/data/*
   
   # Start Zookeeper
   /opt/homebrew/bin/zkServer start-foreground
   
   # Verify in another terminal
   echo "ruok" | nc localhost 2181
   # Should return "imok"
   ```

3. **Start Kafka**:
   ```bash
   /opt/homebrew/opt/kafka/bin/kafka-server-start /opt/homebrew/etc/kafka/server.properties
   
   # Verify topic exists
   /opt/homebrew/opt/kafka/bin/kafka-topics --describe --topic log-events --bootstrap-server localhost:9092
   ```

4. **Start Spring Boot Application**:
   ```bash
   mvn spring-boot:run
   ```

### 6.4 Post-Restart Data Verification

1. Verify pre-restart data is accessible:
```graphql
query {
  logs(
    filter: {
      metadata: {
        key: "test_type",
        value: "restart"
      }
    }
  ) {
    content {
      id
      message
      metadata {
        key
        value
      }
    }
  }
}
```

2. Add new test log:
```graphql
mutation {
  ingestLog(input: {
    timestamp: "2024-03-17T14:00:00Z",
    application: "restart-test",
    message: "Post-restart test log",
    severity: INFO,
    metadata: [
      { key: "test_type", value: "restart" },
      { key: "phase", value: "post-restart" }
    ]
  }) {
    id
  }
}
```

3. Verify both pre and post-restart data:
```graphql
query {
  logs(
    filter: {
      applications: ["restart-test"]
    }
  ) {
    content {
      id
      message
      metadata {
        key
        value
      }
    }
    totalElements
  }
}
```

### 6.5 Troubleshooting Failed Restarts

1. **Zookeeper Connection Issues**:
   ```bash
   # Clear Zookeeper data
   rm -rf /opt/homebrew/var/run/zookeeper/data/*
   
   # Kill any lingering processes
   pkill -f zookeeper
   pkill -f kafka
   
   # Restart in correct order
   /opt/homebrew/bin/zkServer start-foreground
   # Wait for success message
   ```

2. **Kafka Connection Issues**:
   ```bash
   # Check Zookeeper first
   echo "ruok" | nc localhost 2181
   
   # Clear Kafka logs if needed
   rm -rf /opt/homebrew/var/lib/kafka-logs/*
   
   # Restart Kafka
   /opt/homebrew/opt/kafka/bin/kafka-server-start /opt/homebrew/etc/kafka/server.properties
   ```

3. **Elasticsearch Issues**:
   ```bash
   # Check cluster health
   curl -X GET "localhost:9200/_cluster/health"
   
   # List indices
   curl -X GET "localhost:9200/_cat/indices?v"
   ```

Remember to always check application logs for specific error messages when troubleshooting restart issues. 

## 7. Health Monitoring Testing

### 7.1 Testing the Health Dashboard

1. Access the health dashboard:
   ```
   http://localhost:8080/system-health
   ```

2. Verify the following components are displayed:
   - Overall system health status
   - Database health status
   - Elasticsearch health status
   - Kafka health status
   - Zookeeper health status
   - Detailed metrics section with disk space and memory usage

3. Test the refresh button:
   - Click the "Refresh" button
   - Verify the timestamp updates
   - Verify all health statuses refresh
   - Confirm metrics data updates

### 7.2 Testing Component Health Status

1. Test with all services running:
   - All health indicators should show "UP" status
   - Overall status should be "UP"
   - Disk space and memory metrics should display current usage with progress bars

2. Test with individual services down:
   - Stop Elasticsearch:
     ```bash
     brew services stop elasticsearch
     ```
   - Refresh the health dashboard
   - Elasticsearch status should show "DOWN"
   - Overall status should show "DOWN"
   - Resource metrics should still be visible
   - Restart Elasticsearch:
     ```bash
     brew services start elasticsearch
     ```
   - Wait for service to start (~15 seconds)
   - Refresh dashboard and verify status returns to "UP"

3. Repeat for other services:
   - Test Kafka health by stopping/starting the Kafka broker
   - Test Zookeeper health by stopping/starting the Zookeeper service
   - Test Database health by temporarily changing the database credentials in application.properties

### 7.3 Testing Resource Metrics

1. Test disk space metrics:
   - Verify the disk space usage percentage is displayed correctly
   - Check that free and total space values are shown
   - Confirm progress bar color corresponds to usage level (green for normal, yellow for warning, red for critical)
   - For testing threshold alerts, you can temporarily modify the threshold in application.properties:
     ```
     management.health.diskspace.threshold=100GB
     ```
     (This will trigger a warning if you have less than 100GB free)

2. Test memory metrics:
   - Verify JVM memory usage is displayed correctly
   - Check that used and maximum memory values are shown
   - Confirm progress bar color corresponds to memory usage level
   - Generate load to test dynamic updates:
     ```bash
     # Generate some load with a simple query that loads many records in memory
     for i in {1..20}; do
       curl -s "http://localhost:8080/graphql" -H "Content-Type: application/json" \
         -d '{"query": "query { logs(page: {page: 0, size: 500}) { content { id message } } }" }' > /dev/null
     done
     ```
   - Refresh the dashboard to see memory usage change

### 7.4 Testing the Health API

1. Access the health API endpoint:
   ```bash
   curl -X GET "http://localhost:8080/api/health"
   ```

2. Verify the JSON response includes:
   - Overall status
   - Status for each component
   - Details for each component including:
     - Disk space metrics (total, free, threshold)
     - Memory metrics (used, max)
     - Connection details for services

3. Access specific health components:
   ```bash
   curl -X GET "http://localhost:8080/actuator/health/elasticsearch"
   curl -X GET "http://localhost:8080/actuator/health/db"
   curl -X GET "http://localhost:8080/actuator/health/kafka"
   curl -X GET "http://localhost:8080/actuator/health/diskSpace"
   ```

4. Test the response format with pretty printing:
   ```bash
   curl -X GET "http://localhost:8080/api/health" | jq
   ```

### 7.5 Troubleshooting Health Monitoring

1. If health indicators show "DOWN" status:
   - Check the component's health details for specific error messages
   - Verify the component is actually running
   - Check network connectivity if applicable
   - Review application logs for more detailed error information

2. If metrics display is not working:
   - Check browser console for JavaScript errors
   - Verify that the API endpoint is returning the correct JSON format
   - Try clearing browser cache and refreshing
   - Ensure the correct path is being used for API calls

3. Common health monitoring issues:
   - Incorrect connection strings or credentials
   - Network connectivity problems
   - Resource limitations (memory, disk space)
   - Permission issues with external services
   - Configuration problems in application.properties
   - Mismatched data formats between API and frontend

## 8. API Documentation & Testing

The Log Analyzer includes comprehensive API documentation that can be used for testing all aspects of the system.

### 8.1 Accessing API Documentation

1. API Index:
   ```
   http://localhost:8080/api/index
   ```
   This page provides links to all available documentation and testing interfaces.

2. REST API Documentation (Swagger UI):
   ```
   http://localhost:8080/swagger-ui.html
   ```
   Interactive documentation for all REST endpoints.

3. GraphQL Examples:
   ```
   http://localhost:8080/api-docs/graphql/examples
   ```
   Pre-configured examples of common GraphQL operations.

### 8.2 Testing with Swagger UI

1. Navigate to the Swagger UI:
   ```
   http://localhost:8080/swagger-ui.html
   ```

2. Expand the "Log REST API" section to see available endpoints:
   - Test retrieving logs with various filters
   - Try creating new logs
   - View metadata and application information

3. Each endpoint provides:
   - Request parameter descriptions
   - Required vs. optional fields
   - Response schema information
   - "Try it out" button for live testing

4. To test creating a log:
   - Expand the POST `/api/logs` endpoint
   - Click "Try it out"
   - Modify the example request body
   - Click "Execute"
   - Verify the response status code and body

### 8.3 Testing with GraphiQL

1. Navigate to the GraphiQL interface:
   ```
   http://localhost:8080/graphiql
   ```

2. Use the explorer panel (usually on the left) to browse available:
   - Queries
   - Mutations
   - Subscriptions
   - Types and fields

3. Write a test query:
   ```graphql
   query {
     logs(page: {page: 0, size: 10}) {
       content {
         id
         message
         severity
       }
       totalElements
     }
   }
   ```

4. Click the "Play" button to execute the query
   - Examine the response
   - Modify the query to test different fields
   - Add filters to test search functionality

5. For advanced testing, refer to the GraphQL examples page for more complex query patterns

### 8.4 End-to-End API Testing

1. Create a test workflow combining multiple API calls:
   - Create a new log using the REST API
   - Verify the log exists using a GraphQL query
   - Subscribe to log alerts using the subscription test page
   - Create another log with a matching severity
   - Verify the alert is received via the subscription
   - Check the health dashboard to ensure all components remain healthy

2. Create automated test scripts using tools like Postman, curl, or testing frameworks:
   ```bash
   # Example curl command for creating a log via REST API
   curl -X POST "http://localhost:8080/api/logs" \
     -H "Content-Type: application/json" \
     -d '{
       "timestamp": "2024-03-17T14:32:21Z",
       "application": "test-app",
       "message": "API test log",
       "severity": "INFO",
       "source": "test-script"
     }'
   
   # Example curl command for querying logs via GraphQL
   curl -X POST "http://localhost:8080/graphql" \
     -H "Content-Type: application/json" \
     -d '{
       "query": "query { logs(page: {page: 0, size: 10}) { content { id message } } }"
     }'
   ```

### 8.5 API Documentation Maintenance

1. Keeping documentation up to date:
   - When adding new endpoints, ensure they are properly annotated with OpenAPI annotations
   - Update GraphQL schema when adding new types or operations
   - Add new examples to the GraphQL examples page for new operations
   - Update the API index page when adding new documentation resources 