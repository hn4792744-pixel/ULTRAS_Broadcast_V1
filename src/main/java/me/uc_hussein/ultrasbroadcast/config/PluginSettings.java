package me.uc_hussein.ultrasbroadcast.config;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import me.uc_hussein.ultrasbroadcast.util.GradientUtil;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.FileConfiguration;

/** Typed, cached view of config.yml. Call {@link #load()} after every reload. */
public final class PluginSettings {
    private final UltrasBroadcastPlugin plugin;

    private String defaultLanguage;
    private List<String> languages;
    private boolean playerLanguageChoice;
    private String brandName;
    private String title;
    private String separator;
    private boolean gradientEnabled;
    private String gradientStart;
    private String gradientEnd;
    private final Map<String, TextColor> colors = new HashMap<>();
    private final Map<String, String> symbols = new HashMap<>();
    private long minDurationSeconds;
    private long maxDurationSeconds;
    private int updateIntervalMs;
    private boolean onlyOneGlobal;
    private SoundSpec broadcastSound;
    private final Map<AnnouncementType, Boolean> typeEnabled = new EnumMap<>(AnnouncementType.class);
    private final Map<AnnouncementType, List<String>> typeNames = new EnumMap<>(AnnouncementType.class);
    private String chatLineChar;
    private int chatLineLength;
    private int chatBlankBefore;
    private int chatBlankAfter;
    private long fadeInMs;
    private long fadeOutMs;
    private TextColor timerColor;
    private String timerSuffix;
    private boolean personalWhileGlobal;
    private int cooldownSeconds;
    private boolean previewEnabled;
    private boolean restoreAfterRestart;
    private boolean deleteAllCancelsActive;
    private String databaseFile;
    private int maxAnnouncements;
    private boolean deleteOldestWhenFull;
    private boolean autoDeleteEnabled;
    private int autoDeleteDays;
    private int purgeDeletedDays;
    private final Map<String, Boolean> playerDefaults = new HashMap<>();
    private int guiRefreshTicks;
    private long guiClickCooldownMs;
    private int messagePreviewLength;
    private boolean guiSoundsEnabled;
    private final Map<String, SoundSpec> guiSounds = new HashMap<>();
    private DateTimeFormatter dateFormat;
    private DateTimeFormatter timeFormat;
    private ZoneId zone;

    public PluginSettings(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();
        defaultLanguage = c.getString("language", "en").toLowerCase(Locale.ROOT);
        languages = new ArrayList<>();
        for (String code : c.getStringList("available-languages")) {
            languages.add(code.toLowerCase(Locale.ROOT));
        }
        if (!languages.contains(defaultLanguage)) {
            languages.add(0, defaultLanguage);
        }
        playerLanguageChoice = c.getBoolean("player-language-choice", true);

        brandName = c.getString("branding.name", "ᴜʟᴛʀᴀs");
        title = c.getString("branding.title", "ULTRAS CLUB");
        separator = c.getString("branding.separator", "│");
        gradientEnabled = c.getBoolean("branding.gradient.enabled", true);
        gradientStart = GradientUtil.hexOr(c.getString("branding.gradient.start"), "#ff0000");
        gradientEnd = GradientUtil.hexOr(c.getString("branding.gradient.end"), "#8b0000");

        colors.clear();
        colors.put("accent", GradientUtil.color(c.getString("colors.accent"), TextColor.fromHexString("#c1121f")));
        colors.put("text", GradientUtil.color(c.getString("colors.text"), NamedTextColor.GRAY));
        colors.put("separator", GradientUtil.color(c.getString("colors.separator"), NamedTextColor.DARK_GRAY));
        colors.put("success", GradientUtil.color(c.getString("colors.success"), NamedTextColor.GREEN));
        colors.put("warning", GradientUtil.color(c.getString("colors.warning"), NamedTextColor.YELLOW));
        colors.put("error", GradientUtil.color(c.getString("colors.error"), NamedTextColor.RED));
        colors.put("info", GradientUtil.color(c.getString("colors.info"), NamedTextColor.GRAY));
        colors.put("player", GradientUtil.color(c.getString("colors.player"), NamedTextColor.WHITE));

        symbols.clear();
        symbols.put("success", c.getString("symbols.success", "✓"));
        symbols.put("warning", c.getString("symbols.warning", "!"));
        symbols.put("error", c.getString("symbols.error", "×"));
        symbols.put("info", c.getString("symbols.info", "•"));
        symbols.put("action", c.getString("symbols.action", "→"));

        minDurationSeconds = Math.max(1L, c.getLong("broadcast.minimum-duration", 1L));
        maxDurationSeconds = Math.max(minDurationSeconds, c.getLong("broadcast.maximum-duration", 300L));
        updateIntervalMs = Math.max(50, c.getInt("broadcast.update-interval-ms", 100));
        onlyOneGlobal = c.getBoolean("broadcast.only-one-global-active", true);
        broadcastSound = sound(c, "broadcast.sound", "BLOCK_NOTE_BLOCK_PLING", 1.0F, 0.8F);

        for (AnnouncementType type : AnnouncementType.values()) {
            String base = "types." + type.key();
            typeEnabled.put(type, c.getBoolean(base + ".enabled", true));
            List<String> names = new ArrayList<>();
            for (String name : c.getStringList(base + ".names")) {
                if (!name.isBlank()) {
                    names.add(name.toLowerCase(Locale.ROOT));
                }
            }
            if (names.isEmpty()) {
                names.add(type.key());
            }
            typeNames.put(type, names);
        }

        chatLineChar = c.getString("chat.line-char", "━");
        chatLineLength = Math.max(4, Math.min(80, c.getInt("chat.line-length", 30)));
        chatBlankBefore = Math.max(0, Math.min(3, c.getInt("chat.blank-lines-before", 1)));
        chatBlankAfter = Math.max(0, Math.min(3, c.getInt("chat.blank-lines-after", 1)));
        fadeInMs = Math.max(0L, c.getLong("screen.fade-in-ms", 250L));
        fadeOutMs = Math.max(0L, c.getLong("screen.fade-out-ms", 250L));
        timerColor = GradientUtil.color(c.getString("timer.color"), NamedTextColor.GRAY);
        timerSuffix = c.getString("timer.suffix", "s");

        personalWhileGlobal = c.getBoolean("personal-announcements.allowed-while-global-active", true);
        cooldownSeconds = Math.max(0, c.getInt("cooldown.seconds", 0));
        previewEnabled = c.getBoolean("preview.enabled", false);
        restoreAfterRestart = c.getBoolean("active-announcement.restore-after-restart", false);
        deleteAllCancelsActive = c.getBoolean("delete-all.cancel-active", false);

        String type = c.getString("storage.type", "sqlite");
        if (!"sqlite".equalsIgnoreCase(type)) {
            plugin.getLogger().warning("storage.type '" + type + "' is not supported, using sqlite");
        }
        databaseFile = c.getString("storage.file", "data.db");
        maxAnnouncements = Math.max(1, c.getInt("storage.max-announcements", 500));
        deleteOldestWhenFull = c.getBoolean("storage.delete-oldest-when-full", true);
        autoDeleteEnabled = c.getBoolean("storage.auto-delete.enabled", false);
        autoDeleteDays = Math.max(1, c.getInt("storage.auto-delete.after-days", 30));
        purgeDeletedDays = Math.max(0, c.getInt("storage.purge-deleted-after-days", 0));

        playerDefaults.clear();
        for (String key : List.of("sounds", "chat", "screen", "hearts")) {
            playerDefaults.put(key, c.getBoolean("player-defaults." + key, true));
        }

        guiRefreshTicks = Math.max(0, c.getInt("gui.refresh-ticks", 10));
        guiClickCooldownMs = Math.max(0L, c.getLong("gui.click-cooldown-ms", 250L));
        messagePreviewLength = Math.max(10, c.getInt("gui.message-preview-length", 80));
        guiSoundsEnabled = c.getBoolean("gui-sounds.enabled", true);
        guiSounds.clear();
        guiSounds.put("click", sound(c, "gui-sounds.click", "UI_BUTTON_CLICK", 0.4F, 1.2F));
        guiSounds.put("success", sound(c, "gui-sounds.success", "BLOCK_NOTE_BLOCK_CHIME", 0.6F, 1.5F));
        guiSounds.put("error", sound(c, "gui-sounds.error", "BLOCK_NOTE_BLOCK_BASS", 0.5F, 0.6F));
        guiSounds.put("publish", sound(c, "gui-sounds.publish", "BLOCK_AMETHYST_BLOCK_CHIME", 0.7F, 1.2F));
        guiSounds.put("cancel", sound(c, "gui-sounds.cancel", "BLOCK_LEVER_CLICK", 0.5F, 0.9F));

        dateFormat = formatter(c.getString("format.date", "yyyy-MM-dd"), "yyyy-MM-dd");
        timeFormat = formatter(c.getString("format.time", "HH:mm:ss"), "HH:mm:ss");
        String zoneName = c.getString("format.timezone", "system");
        try {
            zone = "system".equalsIgnoreCase(zoneName) ? ZoneId.systemDefault() : ZoneId.of(zoneName);
        } catch (DateTimeException ex) {
            plugin.getLogger().warning("Invalid format.timezone '" + zoneName + "', using the system zone");
            zone = ZoneId.systemDefault();
        }
    }

    private SoundSpec sound(FileConfiguration c, String path, String defSound, float defVolume, float defPitch) {
        boolean enabled = c.getBoolean(path + ".enabled", true);
        String name = c.getString(path + ".sound", defSound);
        float volume = (float) Math.max(0.0D, Math.min(4.0D, c.getDouble(path + ".volume", defVolume)));
        float pitch = (float) Math.max(0.5D, Math.min(2.0D, c.getDouble(path + ".pitch", defPitch)));
        return new SoundSpec(enabled, name, volume, pitch);
    }

    private DateTimeFormatter formatter(String pattern, String fallback) {
        try {
            return DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid date/time pattern '" + pattern + "'");
            return DateTimeFormatter.ofPattern(fallback);
        }
    }

    /** @return the type for a command word (chat, c, screen ...) or null */
    public AnnouncementType resolveType(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        for (AnnouncementType type : AnnouncementType.values()) {
            if (typeNames.get(type).contains(lower)) {
                return type;
            }
        }
        return null;
    }

    public String primaryName(AnnouncementType type) { return typeNames.get(type).get(0); }
    public boolean typeEnabled(AnnouncementType type) { return typeEnabled.getOrDefault(type, true); }

    public String defaultLanguage() { return defaultLanguage; }
    public List<String> languages() { return languages; }
    public boolean playerLanguageChoice() { return playerLanguageChoice; }
    public String brandName() { return brandName; }
    public String title() { return title; }
    public String separator() { return separator; }
    public boolean gradientEnabled() { return gradientEnabled; }
    public String gradientStart() { return gradientStart; }
    public String gradientEnd() { return gradientEnd; }
    public TextColor color(String key) { return colors.getOrDefault(key, NamedTextColor.GRAY); }
    public String symbol(String key) { return symbols.getOrDefault(key, "•"); }
    public long minDurationMs() { return minDurationSeconds * 1000L; }
    public long maxDurationMs() { return maxDurationSeconds * 1000L; }
    public long minDurationSeconds() { return minDurationSeconds; }
    public long maxDurationSeconds() { return maxDurationSeconds; }
    public int updateIntervalMs() { return updateIntervalMs; }
    public int updateIntervalTicks() { return Math.max(1, Math.round(updateIntervalMs / 50.0F)); }
    public boolean onlyOneGlobal() { return onlyOneGlobal; }
    public SoundSpec broadcastSound() { return broadcastSound; }
    public String chatLineChar() { return chatLineChar; }
    public int chatLineLength() { return chatLineLength; }
    public int chatBlankBefore() { return chatBlankBefore; }
    public int chatBlankAfter() { return chatBlankAfter; }
    public long fadeInMs() { return fadeInMs; }
    public long fadeOutMs() { return fadeOutMs; }
    public TextColor timerColor() { return timerColor; }
    public String timerSuffix() { return timerSuffix; }
    public boolean personalWhileGlobal() { return personalWhileGlobal; }
    public int cooldownSeconds() { return cooldownSeconds; }
    public boolean previewEnabled() { return previewEnabled; }
    public boolean restoreAfterRestart() { return restoreAfterRestart; }
    public boolean deleteAllCancelsActive() { return deleteAllCancelsActive; }
    public String databaseFile() { return databaseFile; }
    public int maxAnnouncements() { return maxAnnouncements; }
    public boolean deleteOldestWhenFull() { return deleteOldestWhenFull; }
    public boolean autoDeleteEnabled() { return autoDeleteEnabled; }
    public int autoDeleteDays() { return autoDeleteDays; }
    public int purgeDeletedDays() { return purgeDeletedDays; }
    public boolean playerDefault(String key) { return playerDefaults.getOrDefault(key, true); }
    public int guiRefreshTicks() { return guiRefreshTicks; }
    public long guiClickCooldownMs() { return guiClickCooldownMs; }
    public int messagePreviewLength() { return messagePreviewLength; }
    public boolean guiSoundsEnabled() { return guiSoundsEnabled; }
    public SoundSpec guiSound(String key) { return guiSounds.get(key); }
    public DateTimeFormatter dateFormat() { return dateFormat; }
    public DateTimeFormatter timeFormat() { return timeFormat; }
    public ZoneId zone() { return zone; }
}
