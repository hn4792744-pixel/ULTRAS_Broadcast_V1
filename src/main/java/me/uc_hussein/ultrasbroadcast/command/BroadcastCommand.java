package me.uc_hussein.ultrasbroadcast.command;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.api.PublishRequest;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import me.uc_hussein.ultrasbroadcast.service.AnnouncementService.CancelResult;
import me.uc_hussein.ultrasbroadcast.service.MessageService;
import me.uc_hussein.ultrasbroadcast.service.MessageService.Level;
import me.uc_hussein.ultrasbroadcast.service.ResultReporter;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** One handler for /bc and /uc_bc: same logic, same service. */
public final class BroadcastCommand implements CommandExecutor {
    static final Pattern NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private final UltrasBroadcastPlugin plugin;

    public BroadcastCommand(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            handle(sender, label, args);
        } catch (RuntimeException ex) {
            plugin.audit().error("COMMAND", "Error while running /" + label + " for " + sender.getName(), ex);
            plugin.messages().send(sender, Level.ERROR, "something-wrong");
        }
        return true;
    }

    private void handle(CommandSender sender, String label, String[] args) {
        MessageService msg = plugin.messages();
        if (!plugin.permissions().has(sender, Perm.USE)) {
            deny(sender, label, args);
            return;
        }
        if (args.length == 0) {
            help(sender, label);
            return;
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        switch (first) {
            case "help" -> help(sender, label);
            case "cancel" -> cancel(sender, label, args);
            case "list" -> list(sender, label, args);
            case "reload" -> reload(sender, label, args);
            default -> {
                AnnouncementType type = plugin.settings().resolveType(first);
                if (type == null) {
                    msg.send(sender, Level.WARNING, "unknown-type");
                } else {
                    publish(sender, label, args, type, Arrays.copyOfRange(args, 1, args.length));
                }
            }
        }
    }

    private void deny(CommandSender sender, String label, String[] args) {
        plugin.messages().send(sender, Level.ERROR, "no-permission");
        String line = "/" + label + (args.length == 0 ? "" : " " + String.join(" ", args));
        plugin.audit().warn("DENIED", sender.getName() + " tried " + (line.length() > 120 ? line.substring(0, 120) + "..." : line));
    }

    // ------------------------------------------------------------------ publish

    private void publish(CommandSender sender, String label, String[] all, AnnouncementType type, String[] rest) {
        MessageService msg = plugin.messages();
        if (!plugin.permissions().has(sender, Perm.PUBLISH)) {
            deny(sender, label, all);
            return;
        }
        if (rest.length == 0) {
            msg.send(sender, Level.WARNING, "empty-text");
            return;
        }
        String last = rest[rest.length - 1];
        double seconds;
        String playerToken = null;
        int textEnd;
        if (NUMBER.matcher(last).matches()) {
            seconds = Double.parseDouble(last);
            textEnd = rest.length - 1;
        } else if (rest.length >= 2 && NUMBER.matcher(rest[rest.length - 2]).matches()) {
            seconds = Double.parseDouble(rest[rest.length - 2]);
            playerToken = last;
            textEnd = rest.length - 2;
        } else {
            msg.send(sender, Level.WARNING, "invalid-number");
            return;
        }
        String text = String.join(" ", Arrays.copyOfRange(rest, 0, textEnd)).strip();
        if (text.isBlank()) {
            msg.send(sender, Level.WARNING, "empty-text");
            return;
        }
        UUID target = null;
        String targetName = null;
        if (playerToken != null && !playerToken.equalsIgnoreCase("all")) {
            Player player = Bukkit.getPlayerExact(playerToken);
            if (player == null) {
                msg.send(sender, Level.WARNING, "player-not-found", msg.player(playerToken));
                return;
            }
            target = player.getUniqueId();
            targetName = player.getName();
        }
        UUID actor = sender instanceof Player p ? p.getUniqueId() : Announcement.CONSOLE_UUID;
        long durationMs = Math.round(seconds * 1000.0D);
        PublishRequest request = new PublishRequest(actor, sender.getName(), type, text, durationMs, target, targetName,
                plugin.permissions().has(sender, Perm.BYPASS_COOLDOWN));
        if (plugin.settings().previewEnabled() && sender instanceof Player player) {
            plugin.gui().openPreview(player, request);
        } else {
            ResultReporter.publishAndReport(plugin, sender, request);
        }
    }

    // ------------------------------------------------------------------ sub commands

    private void cancel(CommandSender sender, String label, String[] args) {
        MessageService msg = plugin.messages();
        if (!plugin.permissions().has(sender, Perm.CANCEL)) {
            deny(sender, label, args);
            return;
        }
        if (args.length > 1) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                msg.send(sender, Level.WARNING, "player-not-found", msg.player(args[1]));
                return;
            }
            CancelResult result = plugin.announcements().cancelPersonal(target.getUniqueId(), sender.getName());
            switch (result.status()) {
                case CANCELLED -> msg.send(sender, Level.SUCCESS, "cancelled-player", msg.player(target.getName()));
                case NONE -> msg.send(sender, Level.WARNING, "no-active");
                case BLOCKED -> msg.send(sender, Level.WARNING, "cancel-blocked");
            }
            return;
        }
        CancelResult result = plugin.announcements().cancelGlobal(sender.getName());
        switch (result.status()) {
            case CANCELLED -> msg.send(sender, Level.SUCCESS, "cancelled");
            case BLOCKED -> msg.send(sender, Level.WARNING, "cancel-blocked");
            case NONE -> {
                if (plugin.announcements().cancelAllPersonal(sender.getName()) > 0) {
                    msg.send(sender, Level.SUCCESS, "cancelled");
                } else {
                    msg.send(sender, Level.WARNING, "no-active");
                }
            }
        }
    }

    private void list(CommandSender sender, String label, String[] args) {
        if (!plugin.permissions().has(sender, Perm.LIST)) {
            deny(sender, label, args);
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, Level.WARNING, "players-only");
            return;
        }
        plugin.gui().openList(player, 0);
    }

    private void reload(CommandSender sender, String label, String[] args) {
        if (!plugin.permissions().has(sender, Perm.RELOAD)) {
            deny(sender, label, args);
            return;
        }
        plugin.reloadAll();
        plugin.audit().log("RELOAD", sender.getName() + " reloaded the plugin");
        plugin.messages().send(sender, Level.SUCCESS, "reloaded");
    }

    private void help(CommandSender sender, String label) {
        MessageService msg = plugin.messages();
        String lang = msg.langOf(sender);
        List<Component> lines = new ArrayList<>();
        lines.add(msg.component(lang, "help.header"));
        if (plugin.permissions().has(sender, Perm.PUBLISH)) {
            lines.add(usage(lang, "/" + label + " <type> <text> <seconds> [player|all]", "help.publish"));
            List<String> names = new ArrayList<>();
            for (AnnouncementType type : AnnouncementType.values()) {
                if (plugin.settings().typeEnabled(type)) {
                    names.add(plugin.settings().primaryName(type));
                }
            }
            lines.add(msg.component(lang, "help.types", Placeholder.unparsed("types", String.join(", ", names))));
        }
        if (plugin.permissions().has(sender, Perm.CANCEL)) {
            lines.add(usage(lang, "/" + label + " cancel [player]", "help.cancel"));
        }
        if (plugin.permissions().has(sender, Perm.LIST) && sender instanceof Player) {
            lines.add(usage(lang, "/" + label + " list", "help.list"));
        }
        if (plugin.permissions().has(sender, Perm.RELOAD)) {
            lines.add(usage(lang, "/" + label + " reload", "help.reload"));
        }
        if (plugin.permissions().has(sender, Perm.SETTINGS) && sender instanceof Player) {
            lines.add(usage(lang, "/bc_setting", "help.settings"));
        }
        for (Component line : lines) {
            sender.sendMessage(line);
        }
    }

    private Component usage(String lang, String command, String descriptionKey) {
        return Component.text()
                .append(Component.text(command, plugin.settings().color("accent")))
                .append(Component.text(" - ", plugin.settings().color("separator")))
                .append(plugin.messages().component(lang, descriptionKey).colorIfAbsent(plugin.settings().color("text")))
                .build();
    }
}
