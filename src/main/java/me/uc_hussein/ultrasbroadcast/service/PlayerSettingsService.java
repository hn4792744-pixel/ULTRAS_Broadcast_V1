package me.uc_hussein.ultrasbroadcast.service;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.model.PlayerSettings;
import me.uc_hussein.ultrasbroadcast.storage.PlayerSettingsRepository;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

/** In-memory cache of player settings backed by SQLite. Loading happens on the async pre-login thread. */
public final class PlayerSettingsService {
    private final UltrasBroadcastPlugin plugin;
    private final PlayerSettingsRepository repository;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();

    public PlayerSettingsService(UltrasBroadcastPlugin plugin, PlayerSettingsRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    private PlayerSettings defaults(UUID uuid, String name) {
        PluginSettings s = plugin.settings();
        return new PlayerSettings(uuid, name, s.defaultLanguage(), s.playerDefault("sounds"), s.playerDefault("chat"),
                s.playerDefault("screen"), s.playerDefault("hearts"), System.currentTimeMillis());
    }

    /** Blocking database read; call only from an async thread (pre-login) or start-up. */
    public void load(UUID uuid, String name) {
        try {
            PlayerSettings loaded = repository.load(uuid);
            cache.put(uuid, loaded != null ? loaded : defaults(uuid, name));
        } catch (SQLException ex) {
            plugin.audit().error("STORAGE", "Could not load settings of " + name, ex);
            cache.put(uuid, defaults(uuid, name));
        }
    }

    /** Never null; players that are not cached yet get the defaults. */
    public PlayerSettings get(UUID uuid) {
        PlayerSettings settings = cache.get(uuid);
        return settings != null ? settings : defaults(uuid, "?");
    }

    public PlayerSettings get(Player player) {
        PlayerSettings settings = cache.get(player.getUniqueId());
        if (settings == null) {
            settings = defaults(player.getUniqueId(), player.getName());
            cache.put(player.getUniqueId(), settings);
        }
        return settings;
    }

    public String language(UUID uuid) {
        PlayerSettings settings = cache.get(uuid);
        String language = settings == null ? plugin.settings().defaultLanguage() : settings.language();
        return plugin.settings().languages().contains(language) ? language : plugin.settings().defaultLanguage();
    }

    /** Keeps the stored name current; saves only when something changed. */
    public void onJoin(Player player) {
        PlayerSettings settings = get(player);
        if (!player.getName().equals(settings.name())) {
            settings.setName(player.getName());
            settings.touch();
            repository.save(settings.copy());
        }
    }

    /** Changes a setting, saves it and writes an audit line. */
    public void update(Player player, String what, Consumer<PlayerSettings> change) {
        PlayerSettings settings = get(player);
        change.accept(settings);
        settings.touch();
        repository.save(settings.copy());
        plugin.audit().log("SETTINGS", player.getName() + " changed " + what);
    }

    public void reset(Player player) {
        PlayerSettings settings = get(player);
        PlayerSettings fresh = defaults(player.getUniqueId(), player.getName());
        for (String key : new String[] {"sounds", "chat", "screen", "hearts"}) {
            settings.set(key, fresh.get(key));
        }
        settings.setLanguage(fresh.language());
        settings.touch();
        repository.save(settings.copy());
        plugin.audit().log("SETTINGS", player.getName() + " reset their settings");
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }
}
