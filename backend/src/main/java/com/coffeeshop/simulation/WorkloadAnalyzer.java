package com.coffeeshop.simulation;

import java.util.List;

public final class WorkloadAnalyzer {

    private static final double OVERLOADED_RATIO = 1.2;
    private static final double UNDERUTILIZED_RATIO = 0.8;

    private WorkloadAnalyzer() {}

    public static void updateWorkloadRatios(List<TestBarista> baristas) {
        double totalWork = baristas.stream().mapToDouble(TestBarista::getCurrentWorkloadMinutes).sum();
        double avgWork = baristas.isEmpty() ? 0 : totalWork / baristas.size();
        for (TestBarista b : baristas) {
            double ratio = avgWork > 0 ? b.getCurrentWorkloadMinutes() / avgWork : 1.0;
            b.setWorkloadRatio(ratio);
        }
    }

    public static double computeBalancePercentage(List<TestBarista> baristas) {
        if (baristas.size() < 2) return 100.0;
        double mean = baristas.stream().mapToDouble(TestBarista::getCurrentWorkloadMinutes).average().orElse(0);
        double variance = baristas.stream()
                .mapToDouble(b -> Math.pow(b.getCurrentWorkloadMinutes() - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double normalized = Math.min(1.0, stdDev / 10.0);
        return (1.0 - normalized) * 100;
    }

    public static double computeBalancePercentageFromCompleted(List<TestBarista> baristas) {
        if (baristas.size() < 2) return 100.0;
        double mean = baristas.stream().mapToDouble(TestBarista::getTotalBusyTimeMinutes).average().orElse(0);
        if (mean <= 0) return 100.0;
        double variance = baristas.stream()
                .mapToDouble(b -> Math.pow(b.getTotalBusyTimeMinutes() - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double normalized = Math.min(1.0, stdDev / (mean + 1));
        return (1.0 - normalized) * 100;
    }

    public static boolean isOverloaded(TestBarista b) {
        return b.getWorkloadRatio() > OVERLOADED_RATIO;
    }

    public static boolean isUnderutilized(TestBarista b) {
        return b.getWorkloadRatio() < UNDERUTILIZED_RATIO;
    }
}
