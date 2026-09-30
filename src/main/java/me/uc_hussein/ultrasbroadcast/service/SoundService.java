package me.uc_hussein.ultrasbroadcast.service;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.config.SoundSpec;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Plays announcement and GUI sounds. Accepts BLOCK_NOTE_BLOCK_PLING style names or block.note_block.pling keys. */
public final class SoundService {
    private final UltrasBroadcastPlugin plugin;
    private final Map<String, Optional<Key>> cache = new ConcurrentHashMap<>();
    private volatile Map<String, Key> enumNames;

    public SoundService(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        cache.clear();
    }

    /** Start-of-announcement sound; honours the player's "Announcement Sounds" setting. */
    public void playAnnouncement(Player player) {
        SoundSpec spec = plugin.settings().broadcastSound();
        if (spec.enabled() && plugin.playerSettings().get(player.getUniqueId()).sounds()) {
            play(player, spec);
        }
    }

    /** GUI sound by key: click, success, error, publish, cancel. */
    public void playGui(Player player, String key) {
        PluginSettings settings = plugin.settings();
        if (!settings.guiSoundsEnabled() || key == null) {
            return;
        }
        SoundSpec spec = settings.guiSound(key);
        if (spec != null && spec.enabled()) {
            play(player, spec);
        }
    }

    private void play(Player player, SoundSpec spec) {
        Optional<Key> key = cache.computeIfAbsent(spec.sound(), this::resolve);
        if (key.isEmpty()) {
            return;
        }
        player.playSound(net.kyori.adventure.sound.Sound.sound(key.get(), net.kyori.adventure.sound.Sound.Source.MASTER,
                spec.volume(), spec.pitch()));
    }

    private Optional<Key> resolve(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        try {
            if (trimmed.contains(".") || trimmed.contains(":")) {
                return Optional.of(Key.key(trimmed.toLowerCase(Locale.ROOT)));
            }
            Key key = enumNames().get(trimmed.toUpperCase(Locale.ROOT));
            if (key != null) {
                return Optional.of(key);
            }
        } catch (RuntimeException ex) {
            // falls through to the warning below
        }
        plugin.audit().warn("CONFIG", "Unknown sound '" + name + "'");
        return Optional.empty();
    }

    private Map<String, Key> enumNames() {
        Map<String, Key> map = enumNames;
        if (map == null) {
            map = new HashMap<>();
            for (Sound sound : Registry.SOUNDS) {
                NamespacedKey key = Registry.SOUNDS.getKey(sound);
                if (key != null) {
                    map.put(key.getKey().toUpperCase(Locale.ROOT).replace('.', '_'), key);
                }
            }
            enumNames = map;
        }
        return map;
    }
}
