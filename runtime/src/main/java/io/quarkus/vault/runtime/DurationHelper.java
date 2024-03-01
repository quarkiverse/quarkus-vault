package io.quarkus.vault.runtime;

import java.time.Duration;

public class DurationHelper {

    public static String toVaultDuration(Duration duration) {
        if (duration == null) {
            return null;
        }
        if (duration.isZero()) {
            return "0";
        }
        var value = duration.toString();
        if (value.startsWith("PT")) {
            value = value.substring(2);
        }
        return value.toLowerCase();
    }

    public static Integer toDurationSeconds(Duration duration) {
        if (duration == null) {
            return null;
        }
        return (int) duration.getSeconds();
    }

    public static Long toLongDurationSeconds(Duration duration) {
        if (duration == null) {
            return null;
        }
        return duration.getSeconds();
    }

    public static String toStringDurationSeconds(Duration duration) {
        if (duration == null) {
            return null;
        }
        return String.valueOf(duration.getSeconds());
    }

    public static Duration fromSeconds(Long seconds) {
        if (seconds == null) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }

    public static Duration fromVaultDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }

    public static Duration fromVaultDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return null;
        }
        return Duration.parse("PT" + duration);
    }

}
