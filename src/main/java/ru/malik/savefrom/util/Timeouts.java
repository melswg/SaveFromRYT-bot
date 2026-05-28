package ru.malik.savefrom.util;

import java.time.Duration;

public final class Timeouts {
    private Timeouts() {
    }

    public static Duration durationFromEnv(String name, long defaultSeconds) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return Duration.ofSeconds(defaultSeconds);
        }

        try {
            long seconds = Long.parseLong(value);
            if (seconds <= 0) {
                return Duration.ofSeconds(defaultSeconds);
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(defaultSeconds);
        }
    }

}
