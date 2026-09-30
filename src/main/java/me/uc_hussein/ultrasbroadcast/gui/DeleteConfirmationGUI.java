package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** "Delete All" confirmation. */
public final class DeleteConfirmationGUI {
    private final UltrasBroadcastPlugin plugin;
    private final ConfirmGUI confirm;

    public DeleteConfirmationGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
        this.confirm = new ConfirmGUI(plugin);
    }

    public void open(Player player, int page) {
        GuiManager gui = plugin.gui();
        int count = plugin.announcements().list().size();
        confirm.open(player, "delete-confirmation", () -> {
            if (!gui.require(player, Perm.DELETE, "delete all announcements")) {
                return;
            }
            int deleted = plugin.announcements().deleteAll(player.getName());
            plugin.messages().send(player, Level.SUCCESS, "deleted-all", Placeholder.unparsed("count", String.valueOf(deleted)));
            plugin.sounds().playGui(player, "success");
            gui.later(() -> gui.openList(player, 0));
        }, () -> gui.later(() -> gui.openList(player, page)), Placeholder.unparsed("count", String.valueOf(count)));
    }
}
