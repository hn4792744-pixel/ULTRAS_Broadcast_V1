package me.uc_hussein.ultrasbroadcast.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeUtil {
    private TimeUtil() {
    }

    /** 5.2 , 0.1 ... (rounded up to tenths so the counter never shows 0.0 while visible). */
    public static String formatRemaining(long ms) {
        long tenths = (Math.max(0L, ms) + 99L) / 100L;
        return (tenths / 10L) + "." + (tenths % 10L);
    }

    /** 10s or 10.5s */
    public static String formatDuration(long ms) {
        if (ms % 1000L == 0L) {
            return (ms / 1000L) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", ms / 1000.0D);
    }

    public static String format(long epochMs, DateTimeFormatter formatter, ZoneId zone) {
        if (epochMs <= 0L) {
            return "-";
        }
        return formatter.format(Instant.ofEpochMilli(epochMs).atZone(zone));
    }
}
