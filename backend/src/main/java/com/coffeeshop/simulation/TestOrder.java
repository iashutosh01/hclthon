package com.coffeeshop.simulation;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestOrder {

    private int id;
    private double arrivalTimeMinutes;
    private int prepTimeMinutes;
    private int complexityScore;
    private int loyaltyBoost;
    private double priorityScore;

    @Builder.Default private String status = "QUEUED";
    private double assignmentTime;
    private double completionTime;
    private int baristaId;

    private double waitTimeMinutes;

    @Builder.Default private int laterArrivalsServedFirst = 0;
    @Builder.Default private boolean fairnessPenaltyApplied = false;
    private String assignmentReason;

    @Builder.Default private boolean alertSent = false;

    @Builder.Default private boolean emergencyBoostCounted = false;

    public double getWaitTimeAt(double currentTime) {
        if (!"QUEUED".equals(status)) return waitTimeMinutes;
        return Math.max(0, currentTime - arrivalTimeMinutes);
    }

    public double getWaitTimeMinutes() {
        return waitTimeMinutes;
    }
}
