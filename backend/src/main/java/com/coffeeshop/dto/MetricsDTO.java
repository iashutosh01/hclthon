package com.coffeeshop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDTO {
    private double avgWaitTimeMinutes;
    private double maxWaitTimeMinutes;
    private double timeoutRate;
    private int fairnessViolations;
    private int totalOrdersProcessed;
    private int totalOrdersCompleted;
    private int queueSize;
    private Instant recordedAt;
}
