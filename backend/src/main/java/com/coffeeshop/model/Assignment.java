package com.coffeeshop.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "assignments", indexes = {
    @Index(name = "idx_assignment_barista", columnList = "barista_id"),
    @Index(name = "idx_assignment_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barista_id", nullable = false)
    private Barista barista;

    private Instant assignedAt;
    private Instant completedAt;

    private String assignmentReason;
}
