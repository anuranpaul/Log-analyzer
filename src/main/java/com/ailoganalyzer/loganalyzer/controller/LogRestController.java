package com.ailoganalyzer.loganalyzer.controller;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.model.Severity;
import com.ailoganalyzer.loganalyzer.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Log REST API", description = "REST endpoints for log operations")
public class LogRestController {

    private final LogService logService;

    @Autowired
    public LogRestController(LogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "Get a log by ID", description = "Returns a log as per the ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved log",
                    content = @Content(schema = @Schema(implementation = Log.class))),
            @ApiResponse(responseCode = "404", description = "Log not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<Log> getLogById(
            @Parameter(description = "ID of the log to be obtained") @PathVariable Long id) {
        return logService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all logs", description = "Returns a paginated list of logs with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved logs")
    })
    @GetMapping
    public ResponseEntity<List<Log>> getLogs(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by application names (comma-separated)") @RequestParam(required = false) Optional<String> applications,
            @Parameter(description = "Filter by severities (comma-separated)") @RequestParam(required = false) Optional<String> severities,
            @Parameter(description = "Filter by start time (ISO format)") @RequestParam(required = false) Optional<String> startTime,
            @Parameter(description = "Filter by end time (ISO format)") @RequestParam(required = false) Optional<String> endTime,
            @Parameter(description = "Filter by metadata key") @RequestParam(required = false) Optional<String> metadataKey,
            @Parameter(description = "Filter by metadata value") @RequestParam(required = false) Optional<String> metadataValue) {

        // If no filters provided, use findAll
        if (applications.isEmpty() && severities.isEmpty() && startTime.isEmpty() && 
            endTime.isEmpty() && metadataKey.isEmpty() && metadataValue.isEmpty()) {
            return ResponseEntity.ok(logService.findAll(page, size).getContent());
        }
        
        // Process filters if any are provided
        List<String> appList = applications.map(apps -> Arrays.asList(apps.split(","))).orElse(Collections.emptyList());
        List<String> sevList = severities.map(sevs -> Arrays.asList(sevs.split(","))).orElse(Collections.emptyList());
        
        // Build filter map for search method
        Map<String, Object> filterParams = new HashMap<>();
        
        if (!appList.isEmpty()) {
            filterParams.put("applications", appList);
        }
        
        if (!sevList.isEmpty()) {
            filterParams.put("severities", sevList);
        }
        
        if (startTime.isPresent()) {
            filterParams.put("startTime", startTime.get());
        }
        
        if (endTime.isPresent()) {
            filterParams.put("endTime", endTime.get());
        }
        
        // Handle metadata filter
        if (metadataKey.isPresent() && metadataValue.isPresent()) {
            Map<String, String> metadataFilter = new HashMap<>();
            metadataFilter.put("key", metadataKey.get());
            metadataFilter.put("value", metadataValue.get());
            filterParams.put("metadata", metadataFilter);
            
            return ResponseEntity.ok(logService.findByMetadata(metadataKey.get(), metadataValue.get(), page, size));
        }
        
        // Use search method for other filters
        List<Log> logs = logService.search(filterParams, page, size);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Create a new log", description = "Creates a new log entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Log created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<Log> createLog(@RequestBody Log log) {
        try {
            Log savedLog = logService.saveLog(log);
            return new ResponseEntity<>(savedLog, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get log metadata", description = "Returns logs with specific metadata")
    @GetMapping("/metadata")
    public ResponseEntity<List<Log>> getLogsByMetadata(
            @Parameter(description = "Metadata key") @RequestParam String key,
            @Parameter(description = "Metadata value") @RequestParam String value,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        List<Log> logs = logService.findByMetadata(key, value, page, size);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get logs by application and severity", description = "Returns logs filtered by application and severity")
    @GetMapping("/filter")
    public ResponseEntity<List<Log>> getLogsByApplicationAndSeverity(
            @Parameter(description = "Application name") @RequestParam String application,
            @Parameter(description = "Severity level") @RequestParam Severity severity) {
        List<Log> logs = logService.findByApplicationAndSeverity(application, severity);
        return ResponseEntity.ok(logs);
    }
} 