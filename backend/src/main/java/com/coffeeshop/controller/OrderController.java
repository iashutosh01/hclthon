package com.coffeeshop.controller;

import com.coffeeshop.dto.BaristaDTO;
import com.coffeeshop.dto.MetricsDTO;
import com.coffeeshop.dto.OrderDTO;
import com.coffeeshop.model.DrinkType;
import com.coffeeshop.model.LoyaltyStatus;
import com.coffeeshop.model.Order;
import com.coffeeshop.service.MetricsService;
import com.coffeeshop.service.OrderService;
import com.coffeeshop.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final MetricsService metricsService;
    private final SimulationService simulationService;

    @GetMapping("/queue")
    public ResponseEntity<List<OrderDTO>> getQueue() {
        return ResponseEntity.ok(orderService.getQueue());
    }

    @GetMapping("/baristas")
    public ResponseEntity<List<BaristaDTO>> getBaristas() {
        return ResponseEntity.ok(orderService.getBaristas());
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsDTO> getMetrics() {
        return ResponseEntity.ok(metricsService.computeCurrentMetrics());
    }


    @GetMapping("/orders/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }


    @PostMapping("/orders")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody Map<String, String> body) {
        String customerName = body.getOrDefault("customerName", "Demo Customer");
        String drink = body.getOrDefault("drinkType", "LATTE");
        String loyalty = body.getOrDefault("loyaltyStatus", "REGULAR");

        DrinkType drinkType = DrinkType.fromString(drink);
        LoyaltyStatus loyaltyStatus = "GOLD".equalsIgnoreCase(loyalty) ? LoyaltyStatus.GOLD : LoyaltyStatus.REGULAR;

        Order order = simulationService.createOrder(customerName, drinkType, loyaltyStatus);
        return ResponseEntity.ok(orderService.toOrderDTO(order, java.time.Instant.now()));
    }
}
