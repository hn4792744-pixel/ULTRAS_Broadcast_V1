package me.uc_hussein.ultrasbroadcast.service;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.api.PublishRequest;
import me.uc_hussein.ultrasbroadcast.api.PublishResult;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.util.TimeUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/** Turns publish results into the calm ULTRAS messages, shared by commands and GUIs. */
public final class ResultReporter {
    private ResultReporter() {
    }

    public static PublishResult publishAndReport(UltrasBroadcastPlugin plugin, CommandSender sender, PublishRequest request) {
        PublishResult result = plugin.announcements().publish(request);
        report(plugin, sender, result, request.targetName(), false);
        return result;
    }

    public static void report(UltrasBroadcastPlugin plugin, CommandSender sender, PublishResult result, String targetName,
                              boolean republish) {
        MessageService msg = plugin.messages();
        PluginSettings s = plugin.settings();
        switch (result.status()) {
            case SUCCESS -> {
                if (republish) {
                    String lang = msg.langOf(sender);
                    msg.send(sender, Level.SUCCESS, "republished", Placeholder.unparsed("id", String.valueOf(result.announcement().id())),
                            Placeholder.component("location", msg.typeName(lang, result.announcement().location())));
                } else if (result.announcement().isGlobal()) {
                    msg.send(sender, Level.SUCCESS, "published");
                } else {
                    msg.send(sender, Level.SUCCESS, "published-player", msg.player(targetName));
                }
            }
            case EMPTY_TEXT -> msg.send(sender, Level.WARNING, "empty-text");
            case TYPE_DISABLED -> msg.send(sender, Level.WARNING, "type-disabled");
            case INVALID_DURATION -> msg.send(sender, Level.WARNING, "duration-range",
                    Placeholder.unparsed("min", String.valueOf(s.minDurationSeconds())),
                    Placeholder.unparsed("max", String.valueOf(s.maxDurationSeconds())));
            case ALREADY_ACTIVE -> msg.send(sender, Level.WARNING, "already-active",
                    Placeholder.unparsed("remaining", TimeUtil.formatRemaining(result.remainingMs())));
            case PERSONAL_BLOCKED -> msg.send(sender, Level.WARNING, "personal-blocked");
            case PLAYER_BUSY -> msg.send(sender, Level.WARNING, "player-busy", msg.player(targetName == null ? "?" : targetName));
            case TARGET_OFFLINE -> msg.send(sender, Level.WARNING, "player-not-found", msg.player(targetName == null ? "?" : targetName));
            case COOLDOWN -> msg.send(sender, Level.WARNING, "cooldown",
                    Placeholder.unparsed("seconds", TimeUtil.formatRemaining(result.remainingMs())));
            case LIST_FULL -> msg.send(sender, Level.WARNING, "list-full");
            case NOT_FOUND -> msg.send(sender, Level.WARNING, "not-found");
            case BLOCKED_BY_EVENT -> msg.send(sender, Level.WARNING, "publish-blocked");
        }
    }
}
