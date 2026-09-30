package me.uc_hussein.ultrasbroadcast.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Converts administrator text (&a, &l, &#RRGGBB ...) into components. Never interpreted as MiniMessage. */
public final class ColorUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private ColorUtil() {
    }

    public static Component parse(String raw) {
        return LEGACY.deserialize(raw == null ? "" : raw);
    }

    public static String plain(String raw) {
        return PlainTextComponentSerializer.plainText().serialize(parse(raw));
    }

    public static String truncate(String text, int max) {
        if (max <= 0 || text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(1, max - 1)) + "…";
    }
}
