package com.coffeeshop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaristaDTO {
    private Long id;
    private String name;
    private double currentWorkloadMinutes;
    private double workloadRatio;
    private boolean available;
    private List<OrderDTO> currentOrders;
}
