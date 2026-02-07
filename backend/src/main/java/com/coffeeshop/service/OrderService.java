package com.coffeeshop.service;

import com.coffeeshop.dto.BaristaDTO;
import com.coffeeshop.dto.OrderDTO;
import com.coffeeshop.model.Order;
import com.coffeeshop.model.OrderStatus;
import com.coffeeshop.model.Barista;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.BaristaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BaristaRepository baristaRepository;
    private final PriorityService priorityService;

    private final List<OrderDTO> accumulatedTestOrders = Collections.synchronizedList(new ArrayList<>());

    public void clearAccumulatedTestOrders() {
        accumulatedTestOrders.clear();
    }

    public void appendRunOrders(List<Order> orders, int runIndex, Instant now) {
        int idx = 0;
        for (Order o : orders) {
            long syntheticId = (long) runIndex * 1000 + idx;
            accumulatedTestOrders.add(toOrderDTO(o, now, syntheticId));
            idx++;
        }
    }

    public List<OrderDTO> getQueue() {
        Instant now = Instant.now();
        List<Order> queue = priorityService.getPrioritizedQueue(now);
        return queue.stream().map(o -> toOrderDTO(o, now)).collect(Collectors.toList());
    }


    public List<OrderDTO> getAllOrders() {
        if (!accumulatedTestOrders.isEmpty()) {
            return new ArrayList<>(accumulatedTestOrders);
        }
        Instant now = Instant.now();
        return orderRepository.findAll().stream()
            .map(o -> toOrderDTO(o, now))
            .collect(Collectors.toList());
    }

    public List<BaristaDTO> getBaristas() {
        Instant now = Instant.now();
        List<Barista> baristas = baristaRepository.findAllByOrderByIdAsc();
        List<BaristaDTO> result = new ArrayList<>();
        for (Barista b : baristas) {
            List<Order> current = orderRepository.findByStatus(OrderStatus.PREPARING).stream()
                .filter(o -> b.equals(o.getBarista()))
                .collect(Collectors.toList());
            BaristaDTO dto = BaristaDTO.builder()
                .id(b.getId())
                .name(b.getName())
                .currentWorkloadMinutes(b.getCurrentWorkloadMinutes())
                .workloadRatio(b.getWorkloadRatio())
                .available(b.isAvailable())
                .currentOrders(current.stream().map(o -> toOrderDTO(o, now)).collect(Collectors.toList()))
                .build();
            result.add(dto);
        }
        return result;
    }

    public OrderDTO toOrderDTO(Order o, Instant now) {
        return toOrderDTO(o, now, o.getId());
    }

    private OrderDTO toOrderDTO(Order o, Instant now, long idOverride) {
        long wait = o.getWaitTimeMinutes(now);
        long eta = estimateWaitTime(o, now);

        return OrderDTO.builder()
            .id(idOverride)
            .customerName(o.getCustomerName())
            .drinkType(o.getDrinkType())
            .loyaltyStatus(o.getLoyaltyStatus())
            .status(o.getStatus())
            .arrivalTime(o.getArrivalTime())
            .assignmentTime(o.getAssignmentTime())
            .completionTime(o.getCompletionTime())
            .baristaId(o.getBarista() != null ? o.getBarista().getId() : null)
            .baristaName(o.getBarista() != null ? o.getBarista().getName() : null)
            .priorityScore(o.getPriorityScore())
            .assignmentReason(o.getAssignmentReason())
            .prepTimeMinutes(o.getPrepTimeMinutes())
            .waitTimeMinutes(wait)
            .estimatedWaitMinutes(eta)
            .build();
    }


    private long estimateWaitTime(Order o, Instant now) {
        if (o.getStatus() == OrderStatus.COMPLETED) return 0;
        if (o.getStatus() == OrderStatus.PREPARING) {
            if (o.getAssignmentTime() == null) return o.getPrepTimeMinutes();
            long elapsed = (now.toEpochMilli() - o.getAssignmentTime().toEpochMilli()) / 60_000;
            return Math.max(0, o.getPrepTimeMinutes() - elapsed);
        }

        List<Order> queue = priorityService.getPrioritizedQueue(now);
        int pos = queue.indexOf(o);
        if (pos < 0) return o.getPrepTimeMinutes();
        int slots = (pos / 3) + 1;
        int avgPrep = 3;
        return slots * avgPrep;
    }
}
