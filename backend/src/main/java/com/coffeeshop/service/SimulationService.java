package com.coffeeshop.service;

import com.coffeeshop.model.*;
import com.coffeeshop.repository.*;
import com.coffeeshop.util.PoissonGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationService {

    private final OrderRepository orderRepository;
    private final BaristaRepository baristaRepository;
    private final AssignmentRepository assignmentRepository;
    private final SchedulerService schedulerService;

    @Value("${coffeeshop.simulation.lambda:1.4}")
    private double lambda;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Random random = new Random();

    private static final String[] NAMES = {"Ashutosh",
            "Aarav", "Aditi", "Aman", "Ananya", "Arjun",
            "Deepak", "Ishita", "Karan", "Neha", "Rahul",
            "Riya", "Rohit", "Sakshi", "Sanjay", "Shreya",
            "Siddharth", "Sneha", "Varun", "Vikram", "Yash"
    };

    private static final DrinkType[] DRINKS = DrinkType.values();

    public boolean isRunning() {
        return running.get();
    }

    @Transactional
    public void startSimulation() {
        if (running.get()) {
            log.info("Simulation already running");
            return;
        }

        if (baristaRepository.count() == 0) {
            for (int i = 1; i <= 3; i++) {
                Barista b = Barista.builder().name("Barista " + i).currentWorkloadMinutes(0).workloadRatio(1.0).build();
                baristaRepository.save(b);
            }
            log.info("Created 3 baristas");
        }

        running.set(true);
        log.info("Simulation STARTED - Poisson λ={}", lambda);
    }

    public void stopSimulation() {
        running.set(false);
        log.info("Simulation STOPPED");
    }


    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void tickSimulation() {
        if (!running.get()) return;

        int arrivals = PoissonGenerator.generate(lambda);
        for (int i = 0; i < arrivals; i++) {
            createRandomOrder();
        }
    }


    @Transactional
    public Order createRandomOrder() {
        String name = NAMES[random.nextInt(NAMES.length)] + " " + (1000 + random.nextInt(9000));
        DrinkType drink = DRINKS[random.nextInt(DRINKS.length)];
        LoyaltyStatus loyalty = random.nextDouble() < 0.2 ? LoyaltyStatus.GOLD : LoyaltyStatus.REGULAR;

        Order order = Order.builder()
            .customerName(name)
            .drinkType(drink)
            .loyaltyStatus(loyalty)
            .status(OrderStatus.QUEUED)
            .arrivalTime(Instant.now())
            .build();
        order = orderRepository.save(order);
        log.debug("New order: {} - {} ({})", order.getId(), order.getCustomerName(), order.getDrinkType());
        return order;
    }


    @Transactional
    public Order createOrder(String customerName, DrinkType drinkType, LoyaltyStatus loyaltyStatus) {
        return createOrderWithArrivalTime(customerName, drinkType, loyaltyStatus, Instant.now());
    }


    @Transactional
    public Order createOrderWithArrivalTime(String customerName, DrinkType drinkType, LoyaltyStatus loyaltyStatus, Instant arrivalTime) {
        Order order = Order.builder()
            .customerName(customerName)
            .drinkType(drinkType)
            .loyaltyStatus(loyaltyStatus != null ? loyaltyStatus : LoyaltyStatus.REGULAR)
            .status(OrderStatus.QUEUED)
            .arrivalTime(arrivalTime)
            .build();
        return orderRepository.save(order);
    }


    @Transactional
    public void resetForTest() {
        assignmentRepository.deleteAll();
        orderRepository.deleteAll();
        if (baristaRepository.count() == 0) {
            for (int i = 1; i <= 3; i++) {
                baristaRepository.save(Barista.builder().name("Barista " + i).currentWorkloadMinutes(0).workloadRatio(1.0).build());
            }
        } else {
            for (Barista b : baristaRepository.findAllByOrderByIdAsc()) {
                b.setCurrentWorkloadMinutes(0);
                b.setWorkloadRatio(1.0);
                baristaRepository.save(b);
            }
        }
    }
}
