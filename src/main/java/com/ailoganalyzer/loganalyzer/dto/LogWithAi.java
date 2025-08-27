package com.ailoganalyzer.loganalyzer.dto;

import com.ailoganalyzer.loganalyzer.model.MetadataField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogWithAi {
    private Long id;
    private Instant timestamp; 
    private String application;
    private String message;
    private String severity;
    private String source;
    private String host;
    private List<MetadataField> metadata;
    private AiResponse ai;
}
