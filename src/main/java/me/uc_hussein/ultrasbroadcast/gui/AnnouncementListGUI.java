package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementStatus;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

/** /bc list - paged list of global announcements. Active ones are green, finished ones gray. */
public final class AnnouncementListGUI {
    private final UltrasBroadcastPlugin plugin;

    public AnnouncementListGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    /** Mutable page number of one open menu. */
    private static final class State {
        int page;
    }

    public void open(Player player, int page) {
        GuiManager gui = plugin.gui();
        GuiHolder holder = gui.create("announcement-list", player);
        State state = new State();
        state.page = Math.max(0, page);
        holder.setRefresher(() -> render(player, holder, state));
        render(player, holder, state);
        gui.open(player, holder);
    }

    private void render(Player player, GuiHolder holder, State state) {
        GuiManager gui = plugin.gui();
        MessageService msg = plugin.messages();
        String lang = msg.langOf(player);
        int[] slots = gui.config("announcement-list").contentSlots();
        int perPage = Math.max(1, slots.length);
        List<Announcement> all = plugin.announcements().list();
        int pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        state.page = Math.max(0, Math.min(state.page, pages - 1));

        holder.clear();
        gui.fill(holder);
        if (all.isEmpty()) {
            gui.place(holder, player, "empty", null, null);
        }
        int start = state.page * perPage;
        for (int i = 0; i < perPage && start + i < all.size(); i++) {
            Announcement a = all.get(start + i);
            long id = a.id();
            boolean active = a.status() == AnnouncementStatus.ACTIVE;
            TagResolver[] tags = AnnouncementInfo.tags(plugin, lang, a);
            gui.placeAt(holder, player, "announcement", active ? "active" : "finished", slots[i], AnnouncementInfo.flags(a),
                    e -> openAnnouncement(player, id, state.page), tags);
        }
        TagResolver[] pageTags = {
                Placeholder.unparsed("page", String.valueOf(state.page + 1)),
                Placeholder.unparsed("pages", String.valueOf(pages)),
                Placeholder.unparsed("count", String.valueOf(all.size()))
        };
        if (state.page > 0) {
            gui.place(holder, player, "previous", null, e -> {
                state.page--;
                render(player, holder, state);
            }, pageTags);
        }
        if (state.page < pages - 1) {
            gui.place(holder, player, "next", null, e -> {
                state.page++;
                render(player, holder, state);
            }, pageTags);
        }
        gui.place(holder, player, "page", null, null, pageTags);
        gui.place(holder, player, "delete-all", null, e -> {
            if (gui.require(player, Perm.DELETE, "delete all announcements")) {
                gui.later(() -> gui.openDeleteAll(player, state.page));
            }
        }, pageTags);
        gui.place(holder, player, "close", null, e -> gui.close(player));
    }

    private void openAnnouncement(Player player, long id, int page) {
        GuiManager gui = plugin.gui();
        Announcement current = plugin.announcements().get(id);
        if (current == null) {
            plugin.messages().send(player, Level.WARNING, "not-found");
            gui.later(() -> gui.openList(player, page));
            return;
        }
        if (current.status() == AnnouncementStatus.ACTIVE) {
            gui.later(() -> gui.openActive(player, id, page));
        } else {
            gui.later(() -> gui.openDetails(player, id, page));
        }
    }
}
