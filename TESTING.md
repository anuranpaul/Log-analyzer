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

## 5. Troubleshooting

### Common Issues and Solutions

1. Kafka Connection Issues:
   - Verify Zookeeper is running: `lsof -i :2181`
   - Check Kafka logs: `tail -f /opt/homebrew/var/log/kafka/kafka.log`
   - Restart Kafka if needed

2. Elasticsearch Issues:
   - Check cluster health: `curl -X GET "localhost:9200/_cluster/health"`
   - Verify index exists: `curl -X GET "localhost:9200/logs"`

3. PostgreSQL Issues:
   - Check database connection: `psql -U your_user -d loganalyzer -c "\dt"`
   - Verify log table: `psql -U your_user -d loganalyzer -c "SELECT COUNT(*) FROM logs;"`

### Verification Commands

1. Check Kafka Topic:
```bash
/opt/homebrew/opt/kafka/bin/kafka-topics --describe --topic log-events --bootstrap-server localhost:9092
```

2. List Elasticsearch Indices:
```bash
curl -X GET "localhost:9200/_cat/indices?v"
```

3. Monitor Kafka Consumer Group:
```bash
/opt/homebrew/opt/kafka/bin/kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group log-analyzer-group
```

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