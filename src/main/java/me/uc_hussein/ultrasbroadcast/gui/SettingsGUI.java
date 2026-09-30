package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.model.PlayerSettings;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** /bc_setting - language, sound and per-type toggles, reset, exit. Only ever edits the viewer's own settings. */
public final class SettingsGUI {
    private static final List<String> TOGGLES = List.of("sounds", "chat", "screen", "hearts");

    private final UltrasBroadcastPlugin plugin;

    public SettingsGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiManager gui = plugin.gui();
        GuiHolder holder = gui.create("settings", player);
        render(player, holder);
        gui.open(player, holder);
    }

    private void render(Player player, GuiHolder holder) {
        GuiManager gui = plugin.gui();
        MessageService msg = plugin.messages();
        String lang = msg.langOf(player);
        PlayerSettings settings = plugin.playerSettings().get(player);
        holder.clear();
        gui.fill(holder);
        gui.place(holder, player, "info", null, null);
        if (plugin.settings().playerLanguageChoice() && plugin.settings().languages().size() > 1) {
            gui.place(holder, player, "language", null, e -> switchLanguage(player),
                    Placeholder.unparsed("language", msg.languageName(lang)));
        }
        for (String key : TOGGLES) {
            boolean enabled = settings.get(key);
            gui.place(holder, player, key, enabled ? "enabled" : "disabled", e -> toggle(player, holder, key),
                    Placeholder.component("state", msg.state(lang, enabled)));
        }
        gui.place(holder, player, "reset", null, e -> {
            plugin.playerSettings().reset(player);
            msg.send(player, Level.SUCCESS, "settings-reset");
            gui.later(() -> open(player));
        });
        gui.place(holder, player, "exit", null, e -> gui.close(player));
    }

    private void toggle(Player player, GuiHolder holder, String key) {
        boolean next = !plugin.playerSettings().get(player).get(key);
        plugin.playerSettings().update(player, key + " to " + (next ? "ON" : "OFF"), s -> s.set(key, next));
        render(player, holder);
    }

    private void switchLanguage(Player player) {
        List<String> languages = plugin.settings().languages();
        String current = plugin.playerSettings().language(player.getUniqueId());
        String next = languages.get((Math.max(0, languages.indexOf(current)) + 1) % languages.size());
        plugin.playerSettings().update(player, "language to " + next, s -> s.setLanguage(next));
        plugin.messages().send(player, Level.SUCCESS, "language-changed",
                Placeholder.unparsed("language", plugin.messages().languageName(next)));
        plugin.gui().later(() -> open(player));
    }
}
