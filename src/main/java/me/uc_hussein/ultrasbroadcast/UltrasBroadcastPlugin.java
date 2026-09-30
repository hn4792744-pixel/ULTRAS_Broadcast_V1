package me.uc_hussein.ultrasbroadcast;

import me.uc_hussein.ultrasbroadcast.api.BroadcastAPI;
import me.uc_hussein.ultrasbroadcast.api.BroadcastAPIImpl;
import me.uc_hussein.ultrasbroadcast.command.BroadcastCommand;
import me.uc_hussein.ultrasbroadcast.command.BroadcastTabCompleter;
import me.uc_hussein.ultrasbroadcast.command.SettingsCommand;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.gui.GuiManager;
import me.uc_hussein.ultrasbroadcast.listener.CommandVisibilityListener;
import me.uc_hussein.ultrasbroadcast.listener.GUIListener;
import me.uc_hussein.ultrasbroadcast.listener.PlayerJoinListener;
import me.uc_hussein.ultrasbroadcast.listener.PlayerQuitListener;
import me.uc_hussein.ultrasbroadcast.logging.AuditLogger;
import me.uc_hussein.ultrasbroadcast.service.AnnouncementService;
import me.uc_hussein.ultrasbroadcast.service.BroadcastRenderer;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.PlayerSettingsService;
import me.uc_hussein.ultrasbroadcast.service.SoundService;
import me.uc_hussein.ultrasbroadcast.storage.AnnouncementRepository;
import me.uc_hussein.ultrasbroadcast.storage.DatabaseManager;
import me.uc_hussein.ultrasbroadcast.storage.PlayerSettingsRepository;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/** ULTRAS_Broadcast_V1 - administrative announcements (chat, screen, hearts). Developer: UC_Hussein. */
public final class UltrasBroadcastPlugin extends JavaPlugin {
    private PluginSettings settings;
    private AuditLogger audit;
    private PermissionUtil permissions;
    private DatabaseManager database;
    private PlayerSettingsService playerSettings;
    private MessageService messages;
    private SoundService sounds;
    private BroadcastRenderer renderer;
    private AnnouncementService announcements;
    private GuiManager gui;
    private BroadcastAPIImpl api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new PluginSettings(this);
        settings.load();
        audit = new AuditLogger(this);
        audit.start();
        permissions = new PermissionUtil(this);
        permissions.load();

        database = new DatabaseManager(this);
        try {
            database.open(new File(getDataFolder(), settings.databaseFile()));
        } catch (Exception ex) {
            audit.error("STORAGE", "Could not open the SQLite database, disabling the plugin", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        playerSettings = new PlayerSettingsService(this, new PlayerSettingsRepository(database));
        messages = new MessageService(this, playerSettings::language);
        messages.load();
        sounds = new SoundService(this);
        renderer = new BroadcastRenderer(this);
        renderer.reload();
        announcements = new AnnouncementService(this, new AnnouncementRepository(database));
        gui = new GuiManager(this);
        gui.load();

        try {
            announcements.load();
        } catch (Exception ex) {
            audit.error("STORAGE", "Could not load announcements, disabling the plugin", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        BroadcastCommand broadcast = new BroadcastCommand(this);
        BroadcastTabCompleter completer = new BroadcastTabCompleter(this);
        for (String name : new String[] {"bc", "uc_bc"}) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(broadcast);
                command.setTabCompleter(completer);
            }
        }
        PluginCommand settingsCommand = getCommand("bc_setting");
        if (settingsCommand != null) {
            settingsCommand.setExecutor(new SettingsCommand(this));
        }

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandVisibilityListener(this), this);

        // Players that are already online (for example after /reload) get their settings loaded off-thread.
        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> playerSettings.load(player.getUniqueId(), player.getName()));
        }

        api = new BroadcastAPIImpl(this);
        getServer().getServicesManager().register(BroadcastAPI.class, api, this, ServicePriority.Normal);
        checkCommandOwners();
        audit.log("ENABLE", getName() + " " + getPluginMeta().getVersion() + " by UC_Hussein enabled");
    }

    /** Never touches other plugins' commands; only reports when a label belongs to somebody else. */
    private void checkCommandOwners() {
        for (String label : new String[] {"bc", "uc_bc", "bc_setting"}) {
            org.bukkit.command.Command registered = getServer().getCommandMap().getCommand(label);
            PluginCommand ours = getCommand(label);
            if (registered != null && ours != null && registered != ours) {
                audit.warn("COMMAND", "/" + label + " is owned by another plugin. Use /" + getName().toLowerCase(java.util.Locale.ROOT)
                        + ":" + label + " for ULTRAS_Broadcast_V1. The other plugin was not modified.");
            }
        }
    }

    @Override
    public void onDisable() {
        if (announcements != null) {
            announcements.shutdown();
        }
        if (gui != null) {
            gui.shutdown();
        }
        if (api != null) {
            getServer().getServicesManager().unregister(BroadcastAPI.class, api);
        }
        if (database != null) {
            database.close();
        }
        if (audit != null) {
            audit.log("DISABLE", getName() + " disabled");
            audit.stop();
        }
    }

    /** /bc reload: config, languages, GUI files, sounds, timer interval. No restart needed. */
    public void reloadAll() {
        reloadConfig();
        settings.load();
        audit.configure();
        permissions.load();
        messages.load();
        sounds.reload();
        renderer.reload();
        gui.load();
        announcements.schedule();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    public PluginSettings settings() { return settings; }
    public AuditLogger audit() { return audit; }
    public PermissionUtil permissions() { return permissions; }
    public PlayerSettingsService playerSettings() { return playerSettings; }
    public MessageService messages() { return messages; }
    public SoundService sounds() { return sounds; }
    public BroadcastRenderer renderer() { return renderer; }
    public AnnouncementService announcements() { return announcements; }
    public GuiManager gui() { return gui; }
}
