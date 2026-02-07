package com.coffeeshop.simulation;

import java.util.List;

public final class FairnessTracker {

    private static final int MAX_LATER_ARRIVALS_BEFORE_PENALTY = 3;

    private FairnessTracker() {}

    public static void onOrderAssigned(TestOrder assignedOrder, List<TestOrder> queuedOrders) {
        double assignedArrival = assignedOrder.getArrivalTimeMinutes();
        for (TestOrder o : queuedOrders) {
            if (o.getStatus().equals("QUEUED") && o.getArrivalTimeMinutes() < assignedArrival) {
                int count = o.getLaterArrivalsServedFirst() + 1;
                o.setLaterArrivalsServedFirst(count);
                if (count > MAX_LATER_ARRIVALS_BEFORE_PENALTY) {
                    o.setFairnessPenaltyApplied(true);
                }
            }
        }
    }

    public static int countFairnessViolations(List<TestOrder> orders) {
        return (int) orders.stream()
                .filter(o -> o.getLaterArrivalsServedFirst() > MAX_LATER_ARRIVALS_BEFORE_PENALTY)
                .count();
    }
}
