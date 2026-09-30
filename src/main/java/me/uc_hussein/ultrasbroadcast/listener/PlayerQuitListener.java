package me.uc_hussein.ultrasbroadcast.listener;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {
    private final UltrasBroadcastPlugin plugin;

    public PlayerQuitListener(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.announcements().handleQuit(event.getPlayer().getUniqueId());
        plugin.playerSettings().unload(event.getPlayer().getUniqueId());
        plugin.gui().forget(event.getPlayer().getUniqueId());
    }
}
