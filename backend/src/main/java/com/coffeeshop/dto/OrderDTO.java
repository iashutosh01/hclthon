package com.coffeeshop.dto;

import com.coffeeshop.model.DrinkType;
import com.coffeeshop.model.LoyaltyStatus;
import com.coffeeshop.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String customerName;
    private DrinkType drinkType;
    private LoyaltyStatus loyaltyStatus;
    private OrderStatus status;
    private Instant arrivalTime;
    private Instant assignmentTime;
    private Instant completionTime;
    private Long baristaId;
    private String baristaName;
    private Double priorityScore;
    private String assignmentReason;
    private int prepTimeMinutes;
    private long waitTimeMinutes;
    private long estimatedWaitMinutes;
}
