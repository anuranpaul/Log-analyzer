package com.ailoganalyzer.loganalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiResponse {
    private String analysis; // AI's analysis of the log
    private Float confidence; // How confident the AI is (0.0 to 1.0)
}