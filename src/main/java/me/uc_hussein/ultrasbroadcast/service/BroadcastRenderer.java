package me.uc_hussein.ultrasbroadcast.service;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.util.GradientUtil;
import me.uc_hussein.ultrasbroadcast.util.TimeUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

/** Builds the components shown for each announcement type. Text is fixed, only the timer changes. */
public final class BroadcastRenderer {
    private final UltrasBroadcastPlugin plugin;
    private Component titleComponent = Component.empty();
    private Component line = Component.empty();

    public BroadcastRenderer(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        PluginSettings s = plugin.settings();
        titleComponent = GradientUtil.gradient(s.title(), s.gradientEnabled(), s.gradientStart(), s.gradientEnd());
        line = GradientUtil.gradient(s.chatLineChar().repeat(s.chatLineLength()), s.gradientEnabled(),
                s.gradientStart(), s.gradientEnd());
    }

    /** Top line, blank, heading, text, blank, bottom line. */
    public Component chatBlock(String lang, Component text) {
        PluginSettings s = plugin.settings();
        List<Component> lines = new ArrayList<>();
        lines.add(line);
        for (int i = 0; i < s.chatBlankBefore(); i++) {
            lines.add(Component.empty());
        }
        lines.add(plugin.messages().component(lang, "chat.heading"));
        lines.add(text.colorIfAbsent(NamedTextColor.WHITE));
        for (int i = 0; i < s.chatBlankAfter(); i++) {
            lines.add(Component.empty());
        }
        lines.add(line);
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    /** Fixed title (server name) + subtitle (announcement). The timer is sent through the action bar below them. */
    public Title screenTitle(Component text, long remainingMs, boolean fadeIn) {
        PluginSettings s = plugin.settings();
        Title.Times times = Title.Times.times(Duration.ofMillis(fadeIn ? s.fadeInMs() : 0L),
                Duration.ofMillis(Math.max(0L, remainingMs)), Duration.ofMillis(s.fadeOutMs()));
        return Title.title(titleComponent, text.colorIfAbsent(NamedTextColor.WHITE), times);
    }

    public Component timer(long remainingMs) {
        PluginSettings s = plugin.settings();
        return Component.text(TimeUtil.formatRemaining(remainingMs) + s.timerSuffix(), s.timerColor());
    }

    /** "Announcement text   5.2s" on a single action bar line, right above the hearts / hotbar. */
    public Component heartsBar(Component text, long remainingMs) {
        return Component.text()
                .append(text.colorIfAbsent(NamedTextColor.WHITE))
                .append(Component.text("   "))
                .append(timer(remainingMs))
                .build();
    }
}
