package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementStatus;
import me.uc_hussein.ultrasbroadcast.service.AnnouncementService.DeleteStatus;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import me.uc_hussein.ultrasbroadcast.util.TimeUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** Information page of a finished announcement: Republish, Delete, Back. */
public final class AnnouncementDetailsGUI {
    private final UltrasBroadcastPlugin plugin;

    public AnnouncementDetailsGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, long id, int page) {
        GuiManager gui = plugin.gui();
        MessageService msg = plugin.messages();
        Announcement a = plugin.announcements().get(id);
        if (a == null) {
            msg.send(player, Level.WARNING, "not-found");
            gui.openList(player, page);
            return;
        }
        if (a.status() == AnnouncementStatus.ACTIVE) {
            gui.openActive(player, id, page);
            return;
        }
        String lang = msg.langOf(player);
        GuiHolder holder = gui.create("announcement-details", player, Placeholder.unparsed("id", String.valueOf(id)));
        var tags = AnnouncementInfo.tags(plugin, lang, a);
        gui.place(holder, player, "info", null, AnnouncementInfo.flags(a), null, tags);
        gui.place(holder, player, "republish", null, e -> republish(player, id, page));
        gui.place(holder, player, "delete", null, e -> delete(player, id, page));
        gui.place(holder, player, "back", null, e -> gui.later(() -> gui.openList(player, page)));
        gui.open(player, holder);
    }

    private void republish(Player player, long id, int page) {
        GuiManager gui = plugin.gui();
        if (!gui.require(player, Perm.PUBLISH, "republish an announcement")) {
            return;
        }
        if (plugin.announcements().isGlobalActive() && plugin.settings().onlyOneGlobal()) {
            plugin.messages().send(player, Level.WARNING, "already-active", Placeholder.unparsed("remaining",
                    TimeUtil.formatRemaining(plugin.announcements().activeGlobalRemaining())));
            plugin.sounds().playGui(player, "error");
            return;
        }
        gui.later(() -> gui.openPublishLocation(player, id, page));
    }

    private void delete(Player player, long id, int page) {
        GuiManager gui = plugin.gui();
        if (!gui.require(player, Perm.DELETE, "delete an announcement")) {
            return;
        }
        DeleteStatus status = plugin.announcements().delete(player.getName(), id);
        MessageService msg = plugin.messages();
        switch (status) {
            case DELETED -> {
                msg.send(player, Level.SUCCESS, "deleted", Placeholder.unparsed("id", String.valueOf(id)));
                plugin.sounds().playGui(player, "success");
            }
            case NOT_FOUND -> msg.send(player, Level.WARNING, "not-found");
            case ACTIVE -> msg.send(player, Level.WARNING, "delete-active");
            case BLOCKED -> msg.send(player, Level.WARNING, "delete-blocked");
        }
        gui.later(() -> gui.openList(player, page));
    }
}
