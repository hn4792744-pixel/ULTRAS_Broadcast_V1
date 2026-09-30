package me.uc_hussein.ultrasbroadcast.util;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class GradientUtil {
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private GradientUtil() {
    }

    public static String hexOr(String value, String fallback) {
        return value != null && HEX.matcher(value).matches() ? value : fallback;
    }

    public static TextColor color(String value, TextColor fallback) {
        TextColor parsed = value == null ? null : TextColor.fromHexString(value);
        return parsed == null ? fallback : parsed;
    }

    /** Gradient text; with the gradient disabled the start color is used. All Minecraft 1.21+ clients support hex colors. */
    public static Component gradient(String text, boolean enabled, String start, String end) {
        String from = hexOr(start, "#ff0000");
        String to = hexOr(end, "#8b0000");
        String safe = Text.escape(text);
        if (!enabled) {
            return Text.mini("<color:" + from + ">" + safe);
        }
        return Text.mini("<gradient:" + from + ":" + to + ">" + safe + "</gradient>");
    }
}
