package com.coffeeshop.simulation;

import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

public class SimulationEngine {

    private static final int NUM_TEST_CASES = 10;
    private static final int ORDERS_PER_CASE = 250;
    private static final int NUM_BARISTAS = 3;
    private static final double SIM_END_MINUTES = 180;
    private static final double TICK_MINUTES = 0.5;
    private static final double SCHEDULER_INTERVAL = 0.5;

    private static final int[] PREP_TIMES = {1, 2, 2, 4, 4, 6};
    private static final int[] COMPLEXITY_SCORES = {100, 80, 80, 40, 40, 0};
    private static final int[] DRINK_WEIGHTS = {25, 20, 15, 20, 12, 8};

    public static List<TestOrder> generateOrders(int testCaseSeed) {
        Random r = new Random(testCaseSeed);
        List<TestOrder> orders = new ArrayList<>();
        double time = 0;
        double lambda = 1.4;
        for (int i = 0; i < ORDERS_PER_CASE; i++) {
            double u = Math.max(1e-10, 1 - r.nextDouble());
            double interArrival = -Math.log(u) / lambda;
            time += interArrival;
            int idx = pickWeightedIndex(r);
            int prepTime = PREP_TIMES[idx];
            int complexity = COMPLEXITY_SCORES[idx];
            int loyalty = r.nextDouble() < 0.2 ? 15 : 0;
            TestOrder o = TestOrder.builder()
                    .id(i + 1)
                    .arrivalTimeMinutes(time)
                    .prepTimeMinutes(prepTime)
                    .complexityScore(complexity)
                    .loyaltyBoost(loyalty)
                    .status("QUEUED")
                    .build();
            orders.add(o);
        }
        return orders;
    }

    private static int pickWeightedIndex(Random r) {
        int total = 0;
        for (int w : DRINK_WEIGHTS) total += w;
        int roll = r.nextInt(total);
        int acc = 0;
        for (int i = 0; i < DRINK_WEIGHTS.length; i++) {
            acc += DRINK_WEIGHTS[i];
            if (roll < acc) return i;
        }
        return DRINK_WEIGHTS.length - 1;
    }

    public static TestCaseResult runSingleTestCase(int testCaseIndex, List<TestOrder> orders) {
        int alertsSent = 0;
        int emergencyBoostsApplied = 0;

        List<TestBarista> baristas = new ArrayList<>();
        for (int i = 1; i <= NUM_BARISTAS; i++) {
            baristas.add(TestBarista.builder().id(i).name("Barista " + i).build());
        }

        List<TestOrder> queued = new ArrayList<>();
        double currentTime = 0;
        double nextSchedulerTime = 0;

        while (currentTime < SIM_END_MINUTES) {
            for (TestBarista b : baristas) {
                if (b.getCurrentOrder() != null) {
                    double remaining = b.getCurrentWorkloadMinutes() - TICK_MINUTES;
                    b.setCurrentWorkloadMinutes(Math.max(0, remaining));
                    if (b.getCurrentWorkloadMinutes() <= 0) {
                        TestOrder done = b.getCurrentOrder();
                        done.setStatus("COMPLETED");
                        done.setCompletionTime(currentTime);
                        b.setCurrentOrder(null);
                        b.setOrdersCompleted(b.getOrdersCompleted() + 1);
                        double wait = done.getWaitTimeMinutes();
                        b.setTotalWaitTimeMinutes(b.getTotalWaitTimeMinutes() + Math.max(0, wait));
                        b.setTotalBusyTimeMinutes(b.getTotalBusyTimeMinutes() + done.getPrepTimeMinutes());
                    }
                }
            }

            for (TestOrder o : orders) {
                if (o.getStatus().equals("QUEUED") && o.getArrivalTimeMinutes() <= currentTime) {
                    if (!queued.contains(o)) queued.add(o);
                }
            }
            queued.removeIf(o -> !o.getStatus().equals("QUEUED"));

            if (currentTime >= nextSchedulerTime) {
                nextSchedulerTime = currentTime + SCHEDULER_INTERVAL;
                WorkloadAnalyzer.updateWorkloadRatios(baristas);

                for (TestOrder o : queued) {
                    if (PriorityCalculator.isEmergencyBoost(o, currentTime) && !o.isEmergencyBoostCounted()) {
                        o.setEmergencyBoostCounted(true);
                        emergencyBoostsApplied++;
                    }
                    double score = PriorityCalculator.calculate(o, currentTime);
                    o.setPriorityScore(score);
                }
                queued.sort(Comparator.comparingDouble(TestOrder::getPriorityScore).reversed()
                        .thenComparingDouble(TestOrder::getArrivalTimeMinutes));

                for (TestOrder o : queued) {
                    if (ManagerAlertService.checkAndAlert(o, currentTime)) alertsSent++;
                }

                for (TestBarista b : baristas) {
                    if (!b.isAvailable() || queued.isEmpty()) continue;

                    TestOrder next = selectNextOrder(queued, b, currentTime);
                    if (next == null) continue;

                    assignOrder(next, b, currentTime);
                    queued.remove(next);
                    FairnessTracker.onOrderAssigned(next, queued);
                }

                for (Iterator<TestOrder> it = queued.iterator(); it.hasNext(); ) {
                    TestOrder o = it.next();
                    if (PriorityCalculator.shouldForceAssign(o, currentTime)) {
                        TestBarista avail = baristas.stream().filter(TestBarista::isAvailable).findFirst().orElse(null);
                        if (avail != null) {
                            assignOrder(o, avail, currentTime);
                            it.remove();
                            FairnessTracker.onOrderAssigned(o, queued);
                        }
                    }
                }
            }

            currentTime += TICK_MINUTES;

            long completed = orders.stream().filter(o -> o.getStatus().equals("COMPLETED")).count();
            if (completed == orders.size()) break;
        }

        List<TestOrder> completed = orders.stream().filter(o -> o.getStatus().equals("COMPLETED")).collect(Collectors.toList());
        double avgWait = 0;
        double maxWait = 0;
        int ordersExceeded10Min = 0;
        for (TestOrder o : completed) {
            double wait = o.getWaitTimeMinutes();
            avgWait += wait;
            if (wait > maxWait) maxWait = wait;
            if (wait > 10) ordersExceeded10Min++;
        }
        if (!completed.isEmpty()) avgWait /= completed.size();

        double timeoutRate = completed.isEmpty() ? 0 : Math.min(1.0, Math.max(0, (double) ordersExceeded10Min / completed.size()));

        int fairnessViolations = FairnessTracker.countFairnessViolations(orders);
        double balancePct = WorkloadAnalyzer.computeBalancePercentageFromCompleted(baristas);

        alertsSent = Math.min(alertsSent, orders.size());
        ordersExceeded10Min = Math.min(ordersExceeded10Min, orders.size());

        Map<String, Object> perBarista = new LinkedHashMap<>();
        for (TestBarista b : baristas) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("ordersCompleted", b.getOrdersCompleted());
            info.put("totalBusyTime", Math.round(b.getTotalBusyTimeMinutes() * 100.0) / 100.0);
            info.put("avgWaitForBarista", b.getOrdersCompleted() > 0
                    ? Math.round(b.getTotalWaitTimeMinutes() / b.getOrdersCompleted() * 100.0) / 100.0
                    : 0);
            info.put("workloadRatio", Math.round(b.getWorkloadRatio() * 100.0) / 100.0);
            perBarista.put(b.getName(), info);
        }

        return TestCaseResult.builder()
                .testCaseIndex(testCaseIndex)
                .avgWaitTimeMinutes(avgWait)
                .maxWaitTimeMinutes(maxWait)
                .timeoutRate(timeoutRate)
                .fairnessViolations(fairnessViolations)
                .emergencyBoostsApplied(emergencyBoostsApplied)
                .alertsSentToManager(alertsSent)
                .ordersExceeded10Min(ordersExceeded10Min)
                .workloadBalancePercentage(balancePct)
                .totalCompleted(completed.size())
                .perBarista(perBarista)
                .build();
    }

    private static TestOrder selectNextOrder(List<TestOrder> queue, TestBarista barista, double currentTime) {
        if (queue.isEmpty()) return null;

        TestOrder emergency = queue.stream().filter(o -> PriorityCalculator.shouldForceAssign(o, currentTime)).findFirst().orElse(null);
        if (emergency != null) return emergency;

        if (WorkloadAnalyzer.isOverloaded(barista)) {
            return queue.stream().min(Comparator.comparingInt(TestOrder::getPrepTimeMinutes)
                    .thenComparing((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore())))
                    .orElse(queue.get(0));
        }
        if (WorkloadAnalyzer.isUnderutilized(barista)) {
            return queue.stream().max(Comparator.comparingDouble(TestOrder::getPriorityScore)
                    .thenComparingDouble(TestOrder::getArrivalTimeMinutes))
                    .orElse(queue.get(0));
        }
        return queue.stream().max(Comparator.comparingDouble(TestOrder::getPriorityScore)
                .thenComparingDouble(TestOrder::getArrivalTimeMinutes))
                .orElse(queue.get(0));
    }

    private static void assignOrder(TestOrder order, TestBarista barista, double currentTime) {
        order.setStatus("PREPARING");
        order.setBaristaId(barista.getId());
        order.setAssignmentTime(currentTime);
        double wait = Math.max(0, currentTime - order.getArrivalTimeMinutes());
        order.setWaitTimeMinutes(wait);
        StringBuilder reason = new StringBuilder();
        if (wait >= 9.5) reason.append("EMERGENCY: Approaching 10-min timeout. ");
        else if (wait >= 8) reason.append("Urgency: Wait >8 min. ");
        if (WorkloadAnalyzer.isOverloaded(barista)) reason.append("Short order for overloaded barista. ");
        else if (WorkloadAnalyzer.isUnderutilized(barista)) reason.append("Complex order for underutilized barista. ");
        reason.append(String.format("Priority=%.1f, Wait=%.1f min.", order.getPriorityScore(), wait));
        order.setAssignmentReason(reason.toString());
        barista.setCurrentOrder(order);
        barista.setCurrentWorkloadMinutes(order.getPrepTimeMinutes());
    }

    public static SimulationResult runMonteCarlo() {
        List<TestCaseResult> results = new ArrayList<>();
        for (int i = 0; i < NUM_TEST_CASES; i++) {
            List<TestOrder> orders = generateOrders(i);
            TestCaseResult r = runSingleTestCase(i + 1, orders);
            results.add(r);
        }

        double avgWaitOverall = results.stream().mapToDouble(TestCaseResult::getAvgWaitTimeMinutes).average().orElse(0);
        double avgTimeoutRate = Math.min(1.0, Math.max(0,
                results.stream().mapToDouble(TestCaseResult::getTimeoutRate).average().orElse(0)));
        double avgBalancePct = results.stream().mapToDouble(TestCaseResult::getWorkloadBalancePercentage).average().orElse(0);
        int totalFairnessViolations = results.stream().mapToInt(TestCaseResult::getFairnessViolations).sum();
        int totalAlerts = results.stream().mapToInt(TestCaseResult::getAlertsSentToManager).sum();
        int totalOrdersExceeded10Min = results.stream().mapToInt(TestCaseResult::getOrdersExceeded10Min).sum();

        int totalOrdersAllRuns = NUM_TEST_CASES * ORDERS_PER_CASE;
        totalAlerts = Math.min(totalAlerts, totalOrdersAllRuns);
        totalOrdersExceeded10Min = Math.min(totalOrdersExceeded10Min, totalOrdersAllRuns);

        return SimulationResult.builder()
                .numTestCases(NUM_TEST_CASES)
                .ordersPerCase(ORDERS_PER_CASE)
                .avgWaitTimeMinutes(avgWaitOverall)
                .avgTimeoutRate(avgTimeoutRate)
                .avgWorkloadBalancePercentage(avgBalancePct)
                .totalFairnessViolations(totalFairnessViolations)
                .totalAlertsSentToManager(totalAlerts)
                .totalOrdersExceeded10Min(totalOrdersExceeded10Min)
                .targetAvgWaitVsFifo("Target ~4.8 min vs FIFO 6.2 min")
                .targetTimeoutRate("Target ~2.3%")
                .results(results)
                .build();
    }

    @Data
    @Builder
    public static class TestCaseResult {
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

    @Data
    @Builder
    public static class SimulationResult {
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
        private List<TestCaseResult> results;
    }
}
