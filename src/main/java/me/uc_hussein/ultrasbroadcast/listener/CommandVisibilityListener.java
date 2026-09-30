package me.uc_hussein.ultrasbroadcast.listener;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

/**
 * Hides our commands from the client command list of players who cannot use them. A label is only hidden when it
 * really belongs to this plugin, so commands of other plugins are never affected.
 */
public final class CommandVisibilityListener implements Listener {
    private final UltrasBroadcastPlugin plugin;

    public CommandVisibilityListener(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        hideIfOurs(event, "bc", plugin.permissions().has(player, Perm.USE));
        hideIfOurs(event, "uc_bc", plugin.permissions().has(player, Perm.USE));
        hideIfOurs(event, "bc_setting", plugin.permissions().has(player, Perm.SETTINGS));
    }

    private void hideIfOurs(PlayerCommandSendEvent event, String label, boolean allowed) {
        if (allowed) {
            return;
        }
        Command ours = plugin.getCommand(label);
        Command registered = plugin.getServer().getCommandMap().getCommand(label);
        if (ours != null && ours == registered) {
            event.getCommands().remove(label);
        }
        event.getCommands().remove(plugin.getName().toLowerCase(java.util.Locale.ROOT) + ":" + label);
    }
}
