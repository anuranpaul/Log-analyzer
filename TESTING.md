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