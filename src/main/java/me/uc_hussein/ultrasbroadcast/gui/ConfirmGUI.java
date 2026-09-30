package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

/** Shared two-button menu (info, confirm, back) used by delete confirmation and publish preview. */
final class ConfirmGUI {
    private final UltrasBroadcastPlugin plugin;

    ConfirmGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    void open(Player player, String guiName, Runnable onConfirm, Runnable onBack, TagResolver... tags) {
        GuiManager gui = plugin.gui();
        GuiHolder holder = gui.create(guiName, player);
        boolean[] done = {false};
        gui.place(holder, player, "info", null, null, tags);
        gui.place(holder, player, "confirm", null, e -> {
            if (!done[0]) {
                done[0] = true;
                onConfirm.run();
            }
        }, tags);
        gui.place(holder, player, "back", null, e -> {
            if (!done[0]) {
                done[0] = true;
                onBack.run();
            }
        }, tags);
        gui.open(player, holder);
    }
}
