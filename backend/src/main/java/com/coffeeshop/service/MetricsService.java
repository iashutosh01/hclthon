package com.coffeeshop.service;

import com.coffeeshop.model.*;
import com.coffeeshop.repository.*;
import com.coffeeshop.dto.MetricsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MetricsService {

    private final OrderRepository orderRepository;
    private final MetricsRepository metricsRepository;

    private static final int MAX_WAIT_MINUTES = 10;


    public MetricsDTO computeCurrentMetrics() {
        Instant now = Instant.now();
        List<Order> completed = orderRepository.findByStatus(OrderStatus.COMPLETED);
        List<Order> all = orderRepository.findAll();
        List<Order> queued = orderRepository.findByStatus(OrderStatus.QUEUED);

        double avgWait = 0;
        double maxWait = 0;
        int timeoutCount = 0;
        int fairnessViolations = 0;

        for (Order o : completed) {
            if (o.getAssignmentTime() == null) continue;
            long wait = o.getAssignmentTime().toEpochMilli() - o.getArrivalTime().toEpochMilli();
            double waitMin = wait / 60_000.0;
            avgWait += waitMin;
            if (waitMin > maxWait) maxWait = waitMin;
            if (waitMin >= MAX_WAIT_MINUTES) timeoutCount++;
        }
        if (!completed.isEmpty()) {
            avgWait /= completed.size();
        }

        for (Order o : all) {
            if (o.getLaterArrivalsServedFirst() > 3) {
                fairnessViolations++;
            }
        }

        double timeoutRate = completed.isEmpty() ? 0 : (double) timeoutCount / completed.size();

        return MetricsDTO.builder()
            .avgWaitTimeMinutes(avgWait)
            .maxWaitTimeMinutes(maxWait)
            .timeoutRate(timeoutRate)
            .fairnessViolations(fairnessViolations)
            .totalOrdersProcessed(all.size())
            .totalOrdersCompleted(completed.size())
            .queueSize(queued.size())
            .recordedAt(now)
            .build();
    }


    public void recordCompletion(Order order) {
        if (order.getCompletionTime() == null || order.getAssignmentTime() == null) return;
        double avgWait = (order.getAssignmentTime().toEpochMilli() - order.getArrivalTime().toEpochMilli()) / 60_000.0;
        Metrics m = Metrics.builder()
            .recordedAt(Instant.now())
            .avgWaitTimeMinutes(avgWait)
            .maxWaitTimeMinutes(avgWait)
            .timeoutRate(avgWait >= MAX_WAIT_MINUTES ? 1 : 0)
            .fairnessViolations(0)
            .totalOrdersProcessed(1)
            .totalOrdersCompleted(1)
            .build();
        metricsRepository.save(m);
    }


    public void saveSnapshot() {
        MetricsDTO dto = computeCurrentMetrics();
        Metrics m = Metrics.builder()
            .recordedAt(dto.getRecordedAt())
            .avgWaitTimeMinutes(dto.getAvgWaitTimeMinutes())
            .maxWaitTimeMinutes(dto.getMaxWaitTimeMinutes())
            .timeoutRate(dto.getTimeoutRate())
            .fairnessViolations(dto.getFairnessViolations())
            .totalOrdersProcessed(dto.getTotalOrdersProcessed())
            .totalOrdersCompleted(dto.getTotalOrdersCompleted())
            .build();
        metricsRepository.save(m);
    }
}
