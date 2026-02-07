package com.coffeeshop.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "metrics", indexes = {
    @Index(name = "idx_metrics_recorded_at", columnList = "recordedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant recordedAt;

    private double avgWaitTimeMinutes;
    private double maxWaitTimeMinutes;
    private double timeoutRate;
    private int fairnessViolations;
    private int totalOrdersProcessed;
    private int totalOrdersCompleted;
}
