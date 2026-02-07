package com.coffeeshop.service;

import com.coffeeshop.model.Order;
import com.coffeeshop.model.OrderStatus;
import com.coffeeshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class PriorityService {

    private final OrderRepository orderRepository;

    @Value("${coffeeshop.max-wait-minutes:10}")
    private int maxWaitMinutes;

    @Value("${coffeeshop.warning-threshold-minutes:8}")
    private int warningThresholdMinutes;

    @Value("${coffeeshop.emergency-threshold-minutes:9.5}")
    private double emergencyThresholdMinutes;

    private static final int FAIRNESS_PENALTY = 20;
    private static final int MAX_LATER_ARRIVALS_BEFORE_PENALTY = 3;


    public double calculatePriority(Order order, Instant now) {
        long waitMin = order.getWaitTimeMinutes(now);
        int complexityScore = order.getDrinkType().getComplexityScore();
        int loyaltyBoost = order.getLoyaltyStatus().getBoost();

        double waitComponent = Math.min(40, waitMin * 5);
        double complexityComponent = complexityScore * 0.25;
        double loyaltyComponent = Math.min(10, loyaltyBoost);
        double urgencyComponent = 0;
        if (waitMin > warningThresholdMinutes) {
            urgencyComponent = 25 + 50;
        } else if (waitMin > 6) {
            urgencyComponent = 20;
        } else if (waitMin > 4) {
            urgencyComponent = 15;
        } else {
            urgencyComponent = (waitMin / 4.0) * 25;
        }

        double rawScore = waitComponent + complexityComponent + loyaltyComponent + urgencyComponent;

        if (order.getLaterArrivalsServedFirst() > MAX_LATER_ARRIVALS_BEFORE_PENALTY && !order.isFairnessPenaltyApplied()) {
            rawScore -= FAIRNESS_PENALTY;
        }

        double score = Math.min(100, Math.max(0, rawScore));
        return score;
    }


    public void recalculateQueue(Instant now) {
        List<Order> queued = orderRepository.findByStatusOrderByArrivalTimeAsc(OrderStatus.QUEUED);
        for (Order o : queued) {
            double score = calculatePriority(o, now);
            o.setPriorityScore(score);
            orderRepository.save(o);
        }
    }


    public List<Order> getPrioritizedQueue(Instant now) {
        recalculateQueue(now);
        List<Order> queued = orderRepository.findByStatusOrderByArrivalTimeAsc(OrderStatus.QUEUED);
        return queued.stream()
            .sorted(Comparator.comparing(Order::getPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Order::getArrivalTime))
            .collect(Collectors.toList());
    }

    public boolean isEmergency(Order order, Instant now) {
        return order.getWaitTimeMinutes(now) >= emergencyThresholdMinutes;
    }

    public boolean shouldForceAssign(Order order, Instant now) {
        return order.getWaitTimeMinutes(now) >= maxWaitMinutes - 0.5;
    }
}
