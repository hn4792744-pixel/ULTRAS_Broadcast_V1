package me.uc_hussein.ultrasbroadcast.listener;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final UltrasBroadcastPlugin plugin;

    public PlayerJoinListener(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    /** Runs off the main thread, so the database read never lags the server. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        plugin.playerSettings().load(event.getUniqueId(), event.getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.playerSettings().onJoin(event.getPlayer());
    }
}
