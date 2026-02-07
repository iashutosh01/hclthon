package com.coffeeshop.model;


public enum LoyaltyStatus {
    REGULAR(0),
    GOLD(15);

    private final int boost;

    LoyaltyStatus(int boost) {
        this.boost = boost;
    }

    public int getBoost() {
        return boost;
    }
}
