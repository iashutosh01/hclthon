package com.coffeeshop.dto;

import com.coffeeshop.service.TestSimulationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResultDTO {
    private int numTestCases;
    private int ordersPerCase;
    private double avgWaitTimeMinutes;
    private double avgTimeoutRate;
    private double avgWorkloadBalancePercentage;
    private int totalFairnessViolations;
    private int totalAlertsSentToManager;
    private int totalOrdersExceeded10Min;
    private String targetAvgWaitVsFifo;
    private String targetTimeoutRate;
    private List<TestCaseResultDTO> results;
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResultDTO {
        private int testCaseIndex;
        private double avgWaitTimeMinutes;
        private double maxWaitTimeMinutes;
        private double timeoutRate;
        private int fairnessViolations;
        private int emergencyBoostsApplied;
        private int alertsSentToManager;
        private int ordersExceeded10Min;
        private double workloadBalancePercentage;
        private int totalCompleted;
        private Map<String, Object> perBarista;
    }

    public static SimulationResultDTO from(TestSimulationService.SimulationResult r) {
        List<TestCaseResultDTO> dtos = r.getResults().stream().map(tc -> TestCaseResultDTO.builder()
                .testCaseIndex(tc.getTestCaseIndex())
                .avgWaitTimeMinutes(Math.round(tc.getAvgWaitTimeMinutes() * 100.0) / 100.0)
                .maxWaitTimeMinutes(Math.round(tc.getMaxWaitTimeMinutes() * 100.0) / 100.0)
                .timeoutRate(Math.round(tc.getTimeoutRate() * 10000.0) / 10000.0)
                .fairnessViolations(tc.getFairnessViolations())
                .emergencyBoostsApplied(tc.getEmergencyBoostsApplied())
                .alertsSentToManager(tc.getAlertsSentToManager())
                .ordersExceeded10Min(tc.getOrdersExceeded10Min())
                .workloadBalancePercentage(Math.round(tc.getWorkloadBalancePercentage() * 100.0) / 100.0)
                .totalCompleted(tc.getTotalCompleted())
                .perBarista(tc.getPerBarista())
                .build()).toList();

        String summary = String.format(
                "Monte Carlo: %d runs × %d orders. Avg wait: %.2f min (target ~4.8 vs FIFO 6.2). Timeout rate: %.2f%% (target ~2.3%%). Alerts: %d. Orders >10 min: %d. Fairness violations: %d.",
                r.getNumTestCases(), r.getOrdersPerCase(),
                r.getAvgWaitTimeMinutes(), r.getAvgTimeoutRate() * 100,
                r.getTotalAlertsSentToManager(), r.getTotalOrdersExceeded10Min(),
                r.getTotalFairnessViolations());

        return SimulationResultDTO.builder()
                .numTestCases(r.getNumTestCases())
                .ordersPerCase(r.getOrdersPerCase())
                .avgWaitTimeMinutes(Math.round(r.getAvgWaitTimeMinutes() * 100.0) / 100.0)
                .avgTimeoutRate(Math.round(r.getAvgTimeoutRate() * 10000.0) / 10000.0)
                .avgWorkloadBalancePercentage(Math.round(r.getAvgWorkloadBalancePercentage() * 100.0) / 100.0)
                .totalFairnessViolations(r.getTotalFairnessViolations())
                .totalAlertsSentToManager(r.getTotalAlertsSentToManager())
                .totalOrdersExceeded10Min(r.getTotalOrdersExceeded10Min())
                .targetAvgWaitVsFifo(r.getTargetAvgWaitVsFifo())
                .targetTimeoutRate(r.getTargetTimeoutRate())
                .results(dtos)
                .summary(summary)
                .build();
    }
}
