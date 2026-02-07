package com.coffeeshop.simulation;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestBarista {

    private int id;
    private String name;
    @Builder.Default private double currentWorkloadMinutes = 0;
    @Builder.Default private double workloadRatio = 1.0;
    @Builder.Default private double totalWaitTimeMinutes = 0;
    @Builder.Default private double totalBusyTimeMinutes = 0;
    @Builder.Default private int ordersCompleted = 0;

    @Builder.Default
    private TestOrder currentOrder = null;

    public boolean isAvailable() {
        return currentWorkloadMinutes <= 0;
    }
}
