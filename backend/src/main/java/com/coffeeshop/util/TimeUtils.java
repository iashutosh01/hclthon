package com.coffeeshop.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private TimeUtils() {}

    public static long minutesBetween(Instant a, Instant b) {
        if (a == null || b == null) return 0;
        return Math.abs(b.toEpochMilli() - a.toEpochMilli()) / 60_000;
    }

    public static String format(Instant instant) {
        return instant == null ? "-" : FORMATTER.format(instant);
    }
}
