package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.api.PublishRequest;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.service.ResultReporter;
import me.uc_hussein.ultrasbroadcast.util.ColorUtil;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import me.uc_hussein.ultrasbroadcast.util.TimeUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** Optional preview (preview.enabled) shown before a command publishes. */
public final class PreviewGUI {
    private final UltrasBroadcastPlugin plugin;
    private final ConfirmGUI confirm;

    public PreviewGUI(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
        this.confirm = new ConfirmGUI(plugin);
    }

    public void open(Player player, PublishRequest request) {
        GuiManager gui = plugin.gui();
        MessageService msg = plugin.messages();
        String lang = msg.langOf(player);
        String target = request.targetPlayer() == null ? "ALL" : request.targetName();
        confirm.open(player, "preview", () -> {
            gui.close(player);
            if (gui.require(player, Perm.PUBLISH, "publish an announcement")) {
                ResultReporter.publishAndReport(plugin, player, request);
            }
        }, () -> {
            msg.send(player, Level.INFO, "preview-cancelled");
            gui.close(player);
        },
                Placeholder.component("type", msg.typeName(lang, request.type())),
                Placeholder.unparsed("message", ColorUtil.truncate(ColorUtil.plain(request.text()), plugin.settings().messagePreviewLength())),
                Placeholder.unparsed("duration", TimeUtil.formatDuration(request.durationMs())),
                Placeholder.unparsed("target", target));
    }
}
