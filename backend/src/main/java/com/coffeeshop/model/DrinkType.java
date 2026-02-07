package com.coffeeshop.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public enum DrinkType {
    COLD_BREW(1),
    ESPRESSO(2),
    AMERICANO(2),
    CAPPUCCINO(4),
    LATTE(4),
    MOCHA(6);

    private final int prepTimeMinutes;

    DrinkType(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }


    public int getComplexityScore() {
        return Math.max(0, 100 - (prepTimeMinutes - 1) * 20);
    }

    private static final Map<String, DrinkType> BY_NAME = new ConcurrentHashMap<>();
    static {
        for (DrinkType d : values()) {
            BY_NAME.put(d.name().toUpperCase(), d);
        }
    }

    public static DrinkType fromString(String name) {
        return BY_NAME.getOrDefault(name != null ? name.toUpperCase() : "", COLD_BREW);
    }
}
