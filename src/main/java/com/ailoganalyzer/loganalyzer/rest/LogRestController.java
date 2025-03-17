package com.ailoganalyzer.loganalyzer.rest;

import com.ailoganalyzer.loganalyzer.model.Log;
import com.ailoganalyzer.loganalyzer.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogRestController {

    private final LogService logService;

    @PostMapping
    public ResponseEntity<Log> createLog(@RequestBody Log log) {
        Log savedLog = logService.saveLog(log);
        return ResponseEntity.ok(savedLog);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Log> getLog(@PathVariable Long id) {
        Optional<Log> log = logService.findById(id);
        return log.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Log>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(logService.findAll(page, size).getContent());
    }
} 