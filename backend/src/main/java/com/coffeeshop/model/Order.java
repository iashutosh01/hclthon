package com.coffeeshop.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_arrival_time", columnList = "arrivalTime"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_barista_id", columnList = "barista_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrinkType drinkType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoyaltyStatus loyaltyStatus = LoyaltyStatus.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.QUEUED;

    @Column(nullable = false)
    private Instant arrivalTime;

    private Instant assignmentTime;
    private Instant completionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barista_id")
    private Barista barista;

    private Double priorityScore;

    private String assignmentReason;

    @Builder.Default
    private int laterArrivalsServedFirst = 0;

    @Builder.Default
    private boolean fairnessPenaltyApplied = false;

    @Builder.Default
    private boolean alertSent = false;

    public int getPrepTimeMinutes() {
        return drinkType.getPrepTimeMinutes();
    }

    public long getWaitTimeMinutes(Instant now) {
        if (arrivalTime == null || now == null) return 0;
        if (assignmentTime != null) {
            return (assignmentTime.toEpochMilli() - arrivalTime.toEpochMilli()) / 60_000;
        }
        return (now.toEpochMilli() - arrivalTime.toEpochMilli()) / 60_000;
    }
}
