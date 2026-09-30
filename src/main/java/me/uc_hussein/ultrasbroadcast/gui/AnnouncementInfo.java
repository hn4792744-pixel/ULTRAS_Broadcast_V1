package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementStatus;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.util.ColorUtil;
import me.uc_hussein.ultrasbroadcast.util.TimeUtil;
import java.util.Set;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Placeholders shared by the list item lore and the details / active menus. */
final class AnnouncementInfo {
    private AnnouncementInfo() {
    }

    static Set<String> flags(Announcement a) {
        return a.status() == AnnouncementStatus.ACTIVE ? Set.of("active") : Set.of("finished");
    }

    static TagResolver[] tags(UltrasBroadcastPlugin plugin, String lang, Announcement a) {
        MessageService msg = plugin.messages();
        PluginSettings s = plugin.settings();
        long now = System.currentTimeMillis();
        String plain = ColorUtil.truncate(ColorUtil.plain(a.message()), s.messagePreviewLength());
        return new TagResolver[] {
                Placeholder.unparsed("id", String.valueOf(a.id())),
                Placeholder.unparsed("message", plain),
                Placeholder.component("type", msg.typeName(lang, a.type())),
                Placeholder.component("location", msg.typeName(lang, a.location())),
                Placeholder.unparsed("duration", TimeUtil.formatDuration(a.durationMs())),
                Placeholder.unparsed("remaining", TimeUtil.formatRemaining(a.remainingMs(now)) + s.timerSuffix()),
                Placeholder.unparsed("time", TimeUtil.format(a.startedAt(), s.timeFormat(), s.zone())),
                Placeholder.unparsed("date", TimeUtil.format(a.startedAt(), s.dateFormat(), s.zone())),
                Placeholder.unparsed("ended", a.endedAt() > 0L
                        ? TimeUtil.format(a.endedAt(), s.dateFormat(), s.zone()) + " " + TimeUtil.format(a.endedAt(), s.timeFormat(), s.zone())
                        : "-"),
                Placeholder.unparsed("creator", a.creatorName()),
                Placeholder.unparsed("creator_uuid", a.creatorUuid().toString()),
                Placeholder.component("target", msg.component(lang, "target-all")),
                Placeholder.component("status", msg.status(lang, a.status())),
                Placeholder.unparsed("republished", String.valueOf(a.republishCount())),
                Placeholder.unparsed("views", String.valueOf(a.views()))
        };
    }
}
