package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.api.PublishRequest;
import me.uc_hussein.ultrasbroadcast.gui.GuiConfig.ItemDef;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

/** Loads the gui/*.yml files, builds items and inventories, routes clicks and refreshes open menus. */
public final class GuiManager {
    public static final List<String> NAMES = List.of("settings", "announcement-list", "announcement-details",
            "publish-location", "active-announcement", "delete-confirmation", "preview");

    private final UltrasBroadcastPlugin plugin;
    private final Map<String, GuiConfig> configs = new HashMap<>();
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;

    private final SettingsGUI settingsGui;
    private final AnnouncementListGUI listGui;
    private final AnnouncementDetailsGUI detailsGui;
    private final ActiveAnnouncementGUI activeGui;
    private final PublishLocationGUI publishGui;
    private final DeleteConfirmationGUI deleteGui;
    private final PreviewGUI previewGui;

    public GuiManager(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
        this.settingsGui = new SettingsGUI(plugin);
        this.listGui = new AnnouncementListGUI(plugin);
        this.detailsGui = new AnnouncementDetailsGUI(plugin);
        this.activeGui = new ActiveAnnouncementGUI(plugin);
        this.publishGui = new PublishLocationGUI(plugin);
        this.deleteGui = new DeleteConfirmationGUI(plugin);
        this.previewGui = new PreviewGUI(plugin);
    }

    /** (Re)loads every GUI file and restarts the refresh task. */
    public void load() {
        configs.clear();
        for (String name : NAMES) {
            configs.put(name, GuiConfig.load(plugin, name));
        }
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        int ticks = plugin.settings().guiRefreshTicks();
        if (ticks > 0) {
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOpen, ticks, ticks);
        }
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof GuiHolder) {
                player.closeInventory();
            }
        }
    }

    private void refreshOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder(false) instanceof GuiHolder holder && holder.refresher() != null
                    && player.getUniqueId().equals(holder.viewer())) {
                try {
                    holder.refresher().run();
                } catch (RuntimeException ex) {
                    plugin.audit().error("GUI", "Refresh failed", ex);
                    holder.setRefresher(null);
                }
            }
        }
    }

    public GuiConfig config(String name) {
        return configs.get(name);
    }

    // ------------------------------------------------------------------ opening

    public void openSettings(Player player) { settingsGui.open(player); }
    public void openList(Player player, int page) { listGui.open(player, page); }
    public void openDetails(Player player, long id, int page) { detailsGui.open(player, id, page); }
    public void openActive(Player player, long id, int page) { activeGui.open(player, id, page); }
    public void openPublishLocation(Player player, long id, int page) { publishGui.open(player, id, page); }
    public void openDeleteAll(Player player, int page) { deleteGui.open(player, page); }
    public void openPreview(Player player, PublishRequest request) { previewGui.open(player, request); }

    public GuiHolder create(String name, Player player, TagResolver... titleTags) {
        GuiConfig cfg = configs.get(name);
        Component title = plugin.messages().resolveText(plugin.messages().langOf(player), cfg.title(), titleTags);
        GuiHolder holder = new GuiHolder(name, player.getUniqueId());
        holder.attach(Bukkit.createInventory(holder, cfg.rows() * 9, title));
        fill(holder);
        return holder;
    }

    public void open(Player player, GuiHolder holder) {
        player.openInventory(holder.getInventory());
    }

    public void fill(GuiHolder holder) {
        GuiConfig cfg = configs.get(holder.guiName());
        if (cfg.filler() == null) {
            return;
        }
        ItemStack filler = new ItemStack(cfg.filler().material());
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        meta.setHideTooltip(true);
        filler.setItemMeta(meta);
        for (int slot = 0; slot < cfg.rows() * 9; slot++) {
            holder.set(slot, filler, null);
        }
    }

    // ------------------------------------------------------------------ items

    public void place(GuiHolder holder, Player player, String key, String variant, Consumer<InventoryClickEvent> action,
                      TagResolver... tags) {
        place(holder, player, key, variant, Set.of(), action, tags);
    }

    public void place(GuiHolder holder, Player player, String key, String variant, Set<String> flags,
                      Consumer<InventoryClickEvent> action, TagResolver... tags) {
        ItemDef def = configs.get(holder.guiName()).item(key);
        if (def == null || !def.visible()) {
            return;
        }
        for (int slot : def.slots()) {
            placeAt(holder, player, key, variant, slot, flags, action, tags);
        }
    }

    public void placeAt(GuiHolder holder, Player player, String key, String variant, int slot, Set<String> flags,
                        Consumer<InventoryClickEvent> action, TagResolver... tags) {
        ItemDef base = configs.get(holder.guiName()).item(key);
        if (base == null || !base.visible()) {
            return;
        }
        ItemDef def = base.variant(variant);
        ItemStack item = build(def, player, flags, tags);
        Consumer<InventoryClickEvent> wrapped = null;
        if (action != null) {
            String sound = def.sound() != null ? def.sound() : "click";
            wrapped = event -> {
                plugin.sounds().playGui(player, sound);
                action.accept(event);
            };
        }
        holder.set(slot, item, wrapped);
    }

    public ItemStack build(ItemDef def, Player player, Set<String> flags, TagResolver... tags) {
        MessageService msg = plugin.messages();
        String lang = msg.langOf(player);
        ItemStack item = new ItemStack(def.material(), def.amount());
        ItemMeta meta = item.getItemMeta();
        if (def.name() != null) {
            meta.displayName(msg.resolveText(lang, def.name(), tags));
        }
        if (!def.lore().isEmpty()) {
            meta.lore(msg.resolveLore(lang, def.lore(), flags, tags));
        }
        if (def.glow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------------ helpers for the menus

    /** Runs on the next tick so menus are never closed or opened from inside their own click event. */
    public void later(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void close(Player player) {
        later(player::closeInventory);
    }

    /** Checks a permission at click time; explains and logs when it is missing. */
    public boolean require(Player player, Perm perm, String action) {
        if (plugin.permissions().has(player, perm)) {
            return true;
        }
        plugin.messages().send(player, MessageService.Level.ERROR, "no-permission");
        plugin.sounds().playGui(player, "error");
        plugin.audit().warn("DENIED", player.getName() + " tried to " + action + " without permission");
        return false;
    }

    /** false when the click came too fast after the previous one (double-click protection). */
    public boolean allowClick(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = lastClick.put(uuid, now);
        return last == null || now - last >= plugin.settings().guiClickCooldownMs();
    }

    public void forget(UUID uuid) {
        lastClick.remove(uuid);
    }
}
