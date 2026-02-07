package com.coffeeshop.repository;

import com.coffeeshop.model.Order;
import com.coffeeshop.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusOrderByArrivalTimeAsc(OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByArrivalTimeBetween(Instant start, Instant end);

    long countByStatus(OrderStatus status);

    long countByArrivalTimeBetween(Instant start, Instant end);
}
