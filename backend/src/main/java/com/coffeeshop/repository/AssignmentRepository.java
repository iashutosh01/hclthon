package com.coffeeshop.repository;

import com.coffeeshop.model.Assignment;
import com.coffeeshop.model.Barista;
import com.coffeeshop.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByOrder(Order order);

    Optional<Assignment> findByOrderId(Long orderId);

    boolean existsByOrder(Order order);

    long countByBarista(Barista barista);
}
