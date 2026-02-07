package com.coffeeshop.simulation;

public final class ManagerAlertService {

    private static final double ALERT_THRESHOLD_MINUTES = 10.0;

    private ManagerAlertService() {}

    public static boolean checkAndAlert(TestOrder order, double currentTime) {
        if (order.isAlertSent()) return false;
        if (!"QUEUED".equals(order.getStatus())) return false;
        double waitInQueue = currentTime - order.getArrivalTimeMinutes();
        if (waitInQueue >= ALERT_THRESHOLD_MINUTES) {
            order.setAlertSent(true);
            return true;
        }
        return false;
    }

    public static boolean isTimeout(TestOrder order, double currentTime) {
        if (!"QUEUED".equals(order.getStatus())) return false;
        return (currentTime - order.getArrivalTimeMinutes()) >= 10.0;
    }
}
