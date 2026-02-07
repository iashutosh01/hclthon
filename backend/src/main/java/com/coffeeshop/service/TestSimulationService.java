package com.coffeeshop.service;

import com.coffeeshop.model.*;
import com.coffeeshop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class TestSimulationService {

    private final SimulationService simulationService;
    private final SchedulerService schedulerService;
    private final OrderRepository orderRepository;
    private final BaristaRepository baristaRepository;
    private final OrderService orderService;

    private static final int NUM_TEST_CASES = 10;
    private static final int ORDERS_PER_CASE = 250;
    private static final double TICK_MINUTES = 0.5;
    private static final double SIM_END_MINUTES = 180;
    private static final double LAMBDA = 1.4;

    private static final DrinkType[] DRINKS = DrinkType.values();
    private static final int[] DRINK_WEIGHTS = {25, 20, 15, 20, 12, 8};


    @Transactional
    public SimulationResult runMonteCarlo() {
        orderService.clearAccumulatedTestOrders();
        List<TestCaseResult> results = new ArrayList<>();
        for (int i = 0; i < NUM_TEST_CASES; i++) {
            TestCaseResult r = runSingleTestCase(i + 1, i);
            results.add(r);
            orderService.appendRunOrders(orderRepository.findAll(), i + 1, Instant.EPOCH);
        }

        double avgWait = results.stream().mapToDouble(TestCaseResult::getAvgWaitTimeMinutes).average().orElse(0);
        double avgTimeoutRate = results.stream().mapToDouble(TestCaseResult::getTimeoutRate).average().orElse(0);
        double avgBalance = results.stream().mapToDouble(TestCaseResult::getWorkloadBalancePercentage).average().orElse(0);
        int totalFairness = results.stream().mapToInt(TestCaseResult::getFairnessViolations).sum();
        int totalAlerts = results.stream().mapToInt(TestCaseResult::getAlertsSentToManager).sum();
        int totalExceeded10 = results.stream().mapToInt(TestCaseResult::getOrdersExceeded10Min).sum();

        return SimulationResult.builder()
            .numTestCases(NUM_TEST_CASES)
            .ordersPerCase(ORDERS_PER_CASE)
            .avgWaitTimeMinutes(avgWait)
            .avgTimeoutRate(Math.min(1.0, Math.max(0, avgTimeoutRate)))
            .avgWorkloadBalancePercentage(avgBalance)
            .totalFairnessViolations(totalFairness)
            .totalAlertsSentToManager(Math.min(totalAlerts, NUM_TEST_CASES * ORDERS_PER_CASE))
            .totalOrdersExceeded10Min(Math.min(totalExceeded10, NUM_TEST_CASES * ORDERS_PER_CASE))
            .targetAvgWaitVsFifo("Target ~4.8 min vs FIFO 6.2 min")
            .targetTimeoutRate("Target ~2.3%")
            .results(results)
            .build();
    }

    private TestCaseResult runSingleTestCase(int testCaseIndex, int seed) {
        simulationService.resetForTest();
        Instant base = Instant.EPOCH;

        Random r = new Random(seed);
        List<OrderSpec> specs = new ArrayList<>();
        double time = 0;
        for (int i = 0; i < ORDERS_PER_CASE; i++) {
            double u = Math.max(1e-10, 1 - r.nextDouble());
            double interArrival = -Math.log(u) / LAMBDA;
            time += interArrival;
            DrinkType drink = pickWeightedDrink(r);
            LoyaltyStatus loyalty = r.nextDouble() < 0.2 ? LoyaltyStatus.GOLD : LoyaltyStatus.REGULAR;
            specs.add(new OrderSpec(time, drink, loyalty));
        }

        int injected = 0;
        double currentMinutes = 0;
        while (currentMinutes < SIM_END_MINUTES) {
            while (injected < specs.size() && specs.get(injected).arrivalMinutes <= currentMinutes) {
                OrderSpec s = specs.get(injected);
                Instant arrivalTime = base.plus((long) (s.arrivalMinutes * 60_000), ChronoUnit.MILLIS);
                simulationService.createOrderWithArrivalTime("Test " + (injected + 1), s.drink, s.loyalty, arrivalTime);
                injected++;
            }
            Instant simulatedNow = base.plus((long) (currentMinutes * 60_000), ChronoUnit.MILLIS);
            schedulerService.processTick(simulatedNow);
            currentMinutes += TICK_MINUTES;
            long completed = orderRepository.countByStatus(OrderStatus.COMPLETED);
            if (completed >= ORDERS_PER_CASE) break;
        }

        return observeMetrics(testCaseIndex);
    }

    private static class OrderSpec {
        final double arrivalMinutes;
        final DrinkType drink;
        final LoyaltyStatus loyalty;
        OrderSpec(double arrivalMinutes, DrinkType drink, LoyaltyStatus loyalty) {
            this.arrivalMinutes = arrivalMinutes;
            this.drink = drink;
            this.loyalty = loyalty;
        }
    }

    private DrinkType pickWeightedDrink(Random r) {
        int total = 0;
        for (int w : DRINK_WEIGHTS) total += w;
        int roll = r.nextInt(total);
        int acc = 0;
        for (int i = 0; i < DRINKS.length; i++) {
            acc += DRINK_WEIGHTS[i];
            if (roll < acc) return DRINKS[i];
        }
        return DRINKS[DRINKS.length - 1];
    }

    private TestCaseResult observeMetrics(int testCaseIndex) {
        List<Order> completed = orderRepository.findByStatus(OrderStatus.COMPLETED);
        List<Order> all = orderRepository.findAll();
        List<Barista> baristas = baristaRepository.findAllByOrderByIdAsc();

        double avgWait = 0;
        double maxWait = 0;
        int ordersExceeded10Min = 0;
        int alertsSent = 0;
        for (Order o : completed) {
            if (o.getAssignmentTime() == null) continue;
            double wait = (o.getAssignmentTime().toEpochMilli() - o.getArrivalTime().toEpochMilli()) / 60_000.0;
            avgWait += wait;
            if (wait > maxWait) maxWait = wait;
            if (wait > 10) ordersExceeded10Min++;
            if (o.isAlertSent()) alertsSent++;
        }
        if (!completed.isEmpty()) avgWait /= completed.size();

        double timeoutRate = completed.isEmpty() ? 0 : Math.min(1.0, (double) ordersExceeded10Min / completed.size());
        int fairnessViolations = (int) all.stream().filter(o -> o.getLaterArrivalsServedFirst() > 3).count();
        double balancePct = computeWorkloadBalance(baristas);

        Map<String, Object> perBarista = new LinkedHashMap<>();
        for (Barista b : baristas) {
            int ordersCompleted = (int) completed.stream().filter(o -> b.equals(o.getBarista())).count();
            double totalBusyTime = completed.stream().filter(o -> b.equals(o.getBarista()))
                .mapToDouble(Order::getPrepTimeMinutes).sum();
            double totalWaitTime = completed.stream().filter(o -> b.equals(o.getBarista()))
                .filter(o -> o.getAssignmentTime() != null)
                .mapToDouble(o -> (o.getAssignmentTime().toEpochMilli() - o.getArrivalTime().toEpochMilli()) / 60_000.0)
                .sum();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("ordersCompleted", ordersCompleted);
            info.put("totalBusyTime", Math.round(totalBusyTime * 100.0) / 100.0);
            info.put("avgWaitForBarista", ordersCompleted > 0 ? Math.round(totalWaitTime / ordersCompleted * 100.0) / 100.0 : 0);
            info.put("workloadRatio", Math.round(b.getWorkloadRatio() * 100.0) / 100.0);
            perBarista.put(b.getName(), info);
        }

        return TestCaseResult.builder()
            .testCaseIndex(testCaseIndex)
            .avgWaitTimeMinutes(avgWait)
            .maxWaitTimeMinutes(maxWait)
            .timeoutRate(timeoutRate)
            .fairnessViolations(fairnessViolations)
            .emergencyBoostsApplied(0)
            .alertsSentToManager(alertsSent)
            .ordersExceeded10Min(ordersExceeded10Min)
            .workloadBalancePercentage(balancePct)
            .totalCompleted(completed.size())
            .perBarista(perBarista)
            .build();
    }

    private double computeWorkloadBalance(List<Barista> baristas) {
        if (baristas.size() < 2) return 100.0;
        List<Order> completed = orderRepository.findByStatus(OrderStatus.COMPLETED);
        double[] busyTimes = baristas.stream()
            .mapToDouble(b -> completed.stream()
                .filter(o -> b.equals(o.getBarista()))
                .mapToDouble(Order::getPrepTimeMinutes).sum())
            .toArray();
        double mean = Arrays.stream(busyTimes).average().orElse(0);
        if (mean <= 0) return 100.0;
        double variance = Arrays.stream(busyTimes).map(x -> Math.pow(x - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        return Math.max(0, (1.0 - Math.min(1.0, stdDev / (mean + 1))) * 100);
    }

    @lombok.Data
    @lombok.Builder
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

    @lombok.Data
    @lombok.Builder
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
