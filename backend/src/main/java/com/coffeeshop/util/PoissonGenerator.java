package com.coffeeshop.util;

import java.util.Random;

public final class PoissonGenerator {

    private static final Random RANDOM = new Random();

    private PoissonGenerator() {}

    public static int generate(double lambda) {
        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= RANDOM.nextDouble();
        } while (p > L);
        return k - 1;
    }

    public static double generateInterArrivalMinutes(double lambda) {
        return -Math.log(1 - RANDOM.nextDouble()) / lambda;
    }
}
