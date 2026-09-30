package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.api.PublishResult;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.service.ResultReporter;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** Republish: choose Chat / Screen / Hearts, then Publish. Nothing is published until Publish is pressed. */
public final class PublishLocationGUI {
    private final UltrasBroadcastPlugin plugin;

    public PublishLocationGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    private static final class State {
        AnnouncementType chosen;
        boolean done;
    }

    public void open(Player player, long id, int page) {
        GuiManager gui = plugin.gui();
        Announcement a = plugin.announcements().get(id);
        if (a == null) {
            plugin.messages().send(player, Level.WARNING, "not-found");
            gui.openList(player, page);
            return;
        }
        State state = new State();
        state.chosen = plugin.settings().typeEnabled(a.location()) ? a.location() : firstEnabled();
        GuiHolder holder = gui.create("publish-location", player);
        render(player, holder, state, id, page);
        gui.open(player, holder);
    }

    private AnnouncementType firstEnabled() {
        for (AnnouncementType type : AnnouncementType.values()) {
            if (plugin.settings().typeEnabled(type)) {
                return type;
            }
        }
        return AnnouncementType.CHAT;
    }

    private void render(Player player, GuiHolder holder, State state, long id, int page) {
        GuiManager gui = plugin.gui();
        MessageService msg = plugin.messages();
        String lang = msg.langOf(player);
        holder.clear();
        gui.fill(holder);
        gui.place(holder, player, "info", null, null,
                Placeholder.component("location", msg.typeName(lang, state.chosen)),
                Placeholder.unparsed("id", String.valueOf(id)));
        for (AnnouncementType type : AnnouncementType.values()) {
            if (!plugin.settings().typeEnabled(type)) {
                continue;
            }
            gui.place(holder, player, type.key(), type == state.chosen ? "selected" : "unselected", e -> {
                state.chosen = type;
                render(player, holder, state, id, page);
            }, Placeholder.component("location", msg.typeName(lang, type)));
        }
        gui.place(holder, player, "publish", null, e -> publish(player, state, id), Placeholder.component("location", msg.typeName(lang, state.chosen)));
        gui.place(holder, player, "back", null, e -> gui.later(() -> gui.openDetails(player, id, page)));
    }

    private void publish(Player player, State state, long id) {
        GuiManager gui = plugin.gui();
        if (state.done || !gui.require(player, Perm.PUBLISH, "republish an announcement")) {
            return;
        }
        state.done = true;
        boolean bypass = plugin.permissions().has(player, Perm.BYPASS_COOLDOWN);
        PublishResult result = plugin.announcements().republish(player.getUniqueId(), player.getName(), id, state.chosen, bypass);
        ResultReporter.report(plugin, player, result, null, true);
        if (result.success()) {
            plugin.sounds().playGui(player, "publish");
            gui.close(player);
        } else {
            plugin.sounds().playGui(player, "error");
            state.done = false;
        }
    }
}
