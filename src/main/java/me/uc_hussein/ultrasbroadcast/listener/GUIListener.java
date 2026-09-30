package me.uc_hussein.ultrasbroadcast.listener;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.gui.GuiHolder;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Only inventories whose holder is a GuiHolder are handled; every other plugin's inventory is left alone. */
public final class GUIListener implements Listener {
    private final UltrasBroadcastPlugin plugin;

    public GUIListener(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof GuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(holder.viewer())) {
            return;
        }
        if (event.getClickedInventory() != top || !plugin.gui().allowClick(player.getUniqueId())) {
            return;
        }
        Consumer<InventoryClickEvent> action = holder.action(event.getSlot());
        if (action == null) {
            return;
        }
        try {
            action.accept(event);
        } catch (RuntimeException ex) {
            plugin.audit().error("GUI", "Error in menu " + holder.guiName() + " for " + player.getName(), ex);
            plugin.messages().send(player, Level.ERROR, "something-wrong");
            plugin.gui().close(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }
}
