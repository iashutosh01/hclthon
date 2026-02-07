package com.coffeeshop.service;

import com.coffeeshop.model.*;
import com.coffeeshop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {

    private final OrderRepository orderRepository;
    private final BaristaRepository baristaRepository;
    private final AssignmentRepository assignmentRepository;
    private final PriorityService priorityService;
    private final MetricsService metricsService;

    @Value("${coffeeshop.max-wait-minutes:10}")
    private int maxWaitMinutes;

    @Value("${coffeeshop.emergency-threshold-minutes:9.5}")
    private double emergencyThresholdMinutes;

    private static final double OVERLOADED_RATIO = 1.2;
    private static final double UNDERUTILIZED_RATIO = 0.8;


    @Scheduled(fixedRateString = "${coffeeshop.scheduler.interval-seconds:30}000")
    @Transactional
    public void runScheduler() {
        processTick(Instant.now());
    }


    @Transactional
    public void processTick(Instant now) {
        advanceSimulation(now);
        updateBaristaWorkloads(now);
        List<Order> queue = priorityService.getPrioritizedQueue(now);
        List<Barista> baristas = baristaRepository.findAllByOrderByIdAsc();

        for (Barista b : baristas) {
            if (isBaristaAvailable(b, now)) {
                Order next = selectNextOrder(queue, b, now);
                if (next != null) {
                    assignOrder(next, b, now);
                    queue.remove(next);
                }
            }
        }

        for (Order o : new ArrayList<>(queue)) {
            if (priorityService.shouldForceAssign(o, now)) {
                Barista avail = findAvailableBarista(now);
                if (avail != null) {
                    assignOrder(o, avail, now);
                    queue.remove(o);
                }
            }
        }

        for (Order o : orderRepository.findByStatusOrderByArrivalTimeAsc(OrderStatus.QUEUED)) {
            if (o.getWaitTimeMinutes(now) >= emergencyThresholdMinutes && !o.isAlertSent()) {
                o.setAlertSent(true);
                orderRepository.save(o);
                log.warn("ALERT: Order {} (customer {}) has waited {}/{} min - approaching timeout!",
                    o.getId(), o.getCustomerName(), o.getWaitTimeMinutes(now), maxWaitMinutes);
            }
        }
    }

    private void updateBaristaWorkloads(Instant now) {
        List<Barista> baristas = baristaRepository.findAllByOrderByIdAsc();
        double totalWork = 0;
        for (Barista b : baristas) {
            double work = computeCurrentWorkload(b, now);
            b.setCurrentWorkloadMinutes(work);
            totalWork += work;
            baristaRepository.save(b);
        }
        double avgWork = baristas.isEmpty() ? 0 : totalWork / baristas.size();
        for (Barista b : baristas) {
            double ratio = avgWork > 0 ? b.getCurrentWorkloadMinutes() / avgWork : 1.0;
            b.setWorkloadRatio(ratio);
            baristaRepository.save(b);
        }
    }

    private double computeCurrentWorkload(Barista b, Instant now) {
        List<Order> preparing = orderRepository.findByStatus(OrderStatus.PREPARING).stream()
            .filter(o -> b.equals(o.getBarista()))
            .collect(Collectors.toList());
        double work = 0;
        for (Order o : preparing) {
            Instant start = o.getAssignmentTime() != null ? o.getAssignmentTime() : now;
            long elapsed = (now.toEpochMilli() - start.toEpochMilli()) / 60_000;
            int totalPrep = o.getPrepTimeMinutes();
            long remaining = Math.max(0, totalPrep - elapsed);
            work += remaining;
        }
        return work;
    }

    private boolean isBaristaAvailable(Barista b, Instant now) {
        return b.getCurrentWorkloadMinutes() <= 0;
    }

    private Barista findAvailableBarista(Instant now) {
        return baristaRepository.findAllByOrderByIdAsc().stream()
            .filter(b -> isBaristaAvailable(b, now))
            .findFirst()
            .orElse(null);
    }


    private Order selectNextOrder(List<Order> queue, Barista barista, Instant now) {
        if (queue.isEmpty()) return null;

        Order emergency = queue.stream()
            .filter(o -> priorityService.shouldForceAssign(o, now))
            .findFirst()
            .orElse(null);
        if (emergency != null) return emergency;

        double ratio = barista.getWorkloadRatio();
        if (ratio > OVERLOADED_RATIO) {
            return queue.stream()
                .min(Comparator.comparingInt(Order::getPrepTimeMinutes)
                    .thenComparing((a, b) -> Double.compare(
                        (b.getPriorityScore() != null ? b.getPriorityScore() : 0),
                        (a.getPriorityScore() != null ? a.getPriorityScore() : 0))))
                .orElse(queue.get(0));
        } else if (ratio < UNDERUTILIZED_RATIO) {
            return queue.stream()
                .max(Comparator.comparing(Order::getPriorityScore, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Order::getArrivalTime))
                .orElse(queue.get(0));
        }

        return queue.stream()
            .max(Comparator.comparing(Order::getPriorityScore, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Order::getArrivalTime))
            .orElse(queue.get(0));
    }

    @Transactional
    public void assignOrder(Order order, Barista barista, Instant now) {
        String reason = buildAssignmentReason(order, barista, now);
        order.setStatus(OrderStatus.PREPARING);
        order.setBarista(barista);
        order.setAssignmentTime(now);
        order.setAssignmentReason(reason);
        orderRepository.save(order);

        Assignment a = Assignment.builder()
            .order(order)
            .barista(barista)
            .assignedAt(now)
            .assignmentReason(reason)
            .build();
        assignmentRepository.save(a);

        List<Order> earlier = orderRepository.findByStatusOrderByArrivalTimeAsc(OrderStatus.QUEUED).stream()
            .filter(o -> o.getArrivalTime().isBefore(order.getArrivalTime()))
            .collect(Collectors.toList());
        for (Order o : earlier) {
            o.setLaterArrivalsServedFirst(o.getLaterArrivalsServedFirst() + 1);
            if (o.getLaterArrivalsServedFirst() > 3) {
                o.setFairnessPenaltyApplied(true);
            }
            orderRepository.save(o);
        }

        log.info("SCHEDULING: Order {} assigned to barista {} - Reason: {}", order.getId(), barista.getName(), reason);
    }

    private String buildAssignmentReason(Order order, Barista barista, Instant now) {
        long wait = order.getWaitTimeMinutes(now);
        double ratio = barista.getWorkloadRatio();
        var sb = new StringBuilder();

        if (wait >= maxWaitMinutes - 0.5) {
            sb.append("EMERGENCY: Approaching 10-min timeout. ");
        } else if (wait >= 8) {
            sb.append("Urgency: Wait >8 min. ");
        }
        if (ratio > OVERLOADED_RATIO) {
            sb.append("Short order to balance overloaded barista. ");
        } else if (ratio < UNDERUTILIZED_RATIO) {
            sb.append("Complex order to utilize underused barista. ");
        }
        sb.append(String.format("Priority=%.1f, Wait=%d min, Drink=%s.", order.getPriorityScore(), wait, order.getDrinkType()));
        return sb.toString();
    }


    @Transactional
    public void advanceSimulation(Instant now) {
        List<Order> preparing = orderRepository.findByStatus(OrderStatus.PREPARING);
        for (Order o : preparing) {
            if (o.getAssignmentTime() == null) continue;
            long elapsed = (now.toEpochMilli() - o.getAssignmentTime().toEpochMilli()) / 60_000;
            if (elapsed >= o.getPrepTimeMinutes()) {
                completeOrder(o.getId(), now);
            }
        }
    }

    @Transactional
    public void completeOrder(Long orderId, Instant completedAt) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PREPARING) return;

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletionTime(completedAt);
        orderRepository.save(order);

        assignmentRepository.findByOrder(order).ifPresent(a -> {
            a.setCompletedAt(completedAt);
            assignmentRepository.save(a);
        });

        metricsService.recordCompletion(order);
    }
}
