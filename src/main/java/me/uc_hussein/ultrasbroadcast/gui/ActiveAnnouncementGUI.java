package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementStatus;
import me.uc_hussein.ultrasbroadcast.service.AnnouncementService.CancelResult;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

/** Page of an announcement that is still running: information and "Cancel Publication". */
public final class ActiveAnnouncementGUI {
    private final UltrasBroadcastPlugin plugin;

    public ActiveAnnouncementGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, long id, int page) {
        GuiManager gui = plugin.gui();
        Announcement a = plugin.announcements().get(id);
        if (a == null) {
            plugin.messages().send(player, Level.WARNING, "not-found");
            gui.openList(player, page);
            return;
        }
        if (a.status() != AnnouncementStatus.ACTIVE) {
            gui.openDetails(player, id, page);
            return;
        }
        GuiHolder holder = gui.create("active-announcement", player, Placeholder.unparsed("id", String.valueOf(id)));
        holder.setRefresher(() -> render(player, holder, id, page));
        render(player, holder, id, page);
        gui.open(player, holder);
    }

    private void render(Player player, GuiHolder holder, long id, int page) {
        GuiManager gui = plugin.gui();
        MessageService msg = plugin.messages();
        Announcement a = plugin.announcements().get(id);
        if (a == null || a.status() != AnnouncementStatus.ACTIVE) {
            // finished while the menu was open: switch to the matching page instead of staying green
            holder.setRefresher(null);
            gui.later(() -> {
                if (plugin.announcements().get(id) == null) {
                    gui.openList(player, page);
                } else {
                    gui.openDetails(player, id, page);
                }
            });
            return;
        }
        String lang = msg.langOf(player);
        TagResolver[] tags = AnnouncementInfo.tags(plugin, lang, a);
        holder.clear();
        gui.fill(holder);
        gui.place(holder, player, "info", null, AnnouncementInfo.flags(a), null, tags);
        gui.place(holder, player, "cancel", null, e -> cancel(player, page), tags);
        gui.place(holder, player, "back", null, e -> gui.later(() -> gui.openList(player, page)));
    }

    private void cancel(Player player, int page) {
        GuiManager gui = plugin.gui();
        if (!gui.require(player, Perm.CANCEL, "cancel an announcement")) {
            return;
        }
        CancelResult result = plugin.announcements().cancelGlobal(player.getName());
        switch (result.status()) {
            case CANCELLED -> {
                plugin.messages().send(player, Level.SUCCESS, "cancelled");
                plugin.sounds().playGui(player, "cancel");
            }
            case NONE -> plugin.messages().send(player, Level.WARNING, "no-active");
            case BLOCKED -> plugin.messages().send(player, Level.WARNING, "cancel-blocked");
        }
        gui.later(() -> gui.openList(player, page));
    }
}
