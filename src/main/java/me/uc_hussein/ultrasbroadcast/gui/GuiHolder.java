package me.uc_hussein.ultrasbroadcast.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Marks an inventory as an ULTRAS_Broadcast_V1 menu. Clicks are routed by slot, never by item name, and other
 * plugins' inventories are never touched.
 */
public final class GuiHolder implements InventoryHolder {
    private final String guiName;
    private final UUID viewer;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;
    private Runnable refresher;

    public GuiHolder(String guiName, UUID viewer) {
        this.guiName = guiName;
        this.viewer = viewer;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public String guiName() { return guiName; }
    public UUID viewer() { return viewer; }
    public Runnable refresher() { return refresher; }
    public void setRefresher(Runnable refresher) { this.refresher = refresher; }

    public void clear() {
        inventory.clear();
        actions.clear();
    }

    public void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
    }

    public Consumer<InventoryClickEvent> action(int slot) {
        return actions.get(slot);
    }
}
