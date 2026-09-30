package me.uc_hussein.ultrasbroadcast.command;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import me.uc_hussein.ultrasbroadcast.util.PermissionUtil.Perm;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Suggests only what the sender may actually use. */
public final class BroadcastTabCompleter implements TabCompleter {
    private final UltrasBroadcastPlugin plugin;

    public BroadcastTabCompleter(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!plugin.permissions().has(sender, Perm.USE) || args.length == 0) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        String current = args[args.length - 1];
        if (args.length == 1) {
            if (plugin.permissions().has(sender, Perm.PUBLISH)) {
                for (AnnouncementType type : AnnouncementType.values()) {
                    if (plugin.settings().typeEnabled(type)) {
                        options.add(plugin.settings().primaryName(type));
                    }
                }
            }
            if (plugin.permissions().has(sender, Perm.CANCEL)) {
                options.add("cancel");
            }
            if (plugin.permissions().has(sender, Perm.LIST) && sender instanceof Player) {
                options.add("list");
            }
            if (plugin.permissions().has(sender, Perm.RELOAD)) {
                options.add("reload");
            }
            options.add("help");
        } else {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("cancel")) {
                if (args.length == 2 && plugin.permissions().has(sender, Perm.CANCEL)) {
                    options.addAll(playerNames(sender));
                }
            } else if (plugin.settings().resolveType(first) != null && plugin.permissions().has(sender, Perm.PUBLISH)) {
                String previous = args[args.length - 2];
                boolean previousIsNumber = BroadcastCommand.NUMBER.matcher(previous).matches();
                if (previousIsNumber && args.length >= 4) {
                    // <type> <text...> <seconds> [player]
                    options.add("all");
                    options.addAll(playerNames(sender));
                } else if (args.length >= 3 && !previousIsNumber && (current.isEmpty() || Character.isDigit(current.charAt(0)))) {
                    options.addAll(List.of("5", "10", "30", "60"));
                }
            }
        }
        String lower = current.toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(lower));
        return options;
    }

    private List<String> playerNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(sender instanceof Player viewer) || viewer.canSee(player)) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
