package com.coffeeshop.simulation;

public final class PriorityCalculator {

    private static final int WARNING_MINUTES = 8;
    private static final int FAIRNESS_PENALTY = 20;
    private static final int MAX_LATER_ARRIVALS_BEFORE_PENALTY = 3;
    private static final int EMERGENCY_BOOST = 50;

    private PriorityCalculator() {}

    public static double calculate(TestOrder order, double currentTime) {
        if (!"QUEUED".equals(order.getStatus())) return order.getPriorityScore();
        double waitMin = Math.max(0, currentTime - order.getArrivalTimeMinutes());

        double waitComponent = Math.min(40, waitMin * 5);
        double complexityComponent = order.getComplexityScore() * 0.25;
        double loyaltyComponent = Math.min(10, order.getLoyaltyBoost());
        double urgencyComponent;
        if (waitMin > WARNING_MINUTES) {
            urgencyComponent = 25 + EMERGENCY_BOOST;
        } else if (waitMin > 6) {
            urgencyComponent = 20;
        } else if (waitMin > 4) {
            urgencyComponent = 15;
        } else {
            urgencyComponent = (waitMin / 4.0) * 25;
        }

        double raw = waitComponent + complexityComponent + loyaltyComponent + urgencyComponent;

        if (order.getLaterArrivalsServedFirst() > MAX_LATER_ARRIVALS_BEFORE_PENALTY
                && !order.isFairnessPenaltyApplied()) {
            raw -= FAIRNESS_PENALTY;
        }

        return Math.min(100, Math.max(0, raw));
    }

    public static boolean shouldForceAssign(TestOrder order, double currentTime) {
        return order.getWaitTimeAt(currentTime) >= 9.5;
    }

    public static boolean isEmergencyBoost(TestOrder order, double currentTime) {
        return order.getWaitTimeAt(currentTime) > WARNING_MINUTES;
    }
}
