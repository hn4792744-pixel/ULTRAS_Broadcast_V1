package me.uc_hussein.ultrasbroadcast.command;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /bc_setting - players only. */
public final class SettingsCommand implements CommandExecutor {
    private final UltrasBroadcastPlugin plugin;

    public SettingsCommand(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, Level.WARNING, "players-only");
                return true;
            }
            if (!plugin.permissions().has(player, Perm.SETTINGS)) {
                plugin.messages().send(player, Level.ERROR, "no-permission");
                plugin.audit().warn("DENIED", player.getName() + " tried /" + label);
                return true;
            }
            plugin.gui().openSettings(player);
        } catch (RuntimeException ex) {
            plugin.audit().error("COMMAND", "Error while running /" + label + " for " + sender.getName(), ex);
            plugin.messages().send(sender, Level.ERROR, "something-wrong");
        }
        return true;
    }
}
