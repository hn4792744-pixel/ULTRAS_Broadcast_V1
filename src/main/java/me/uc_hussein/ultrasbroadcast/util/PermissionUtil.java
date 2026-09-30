package me.uc_hussein.ultrasbroadcast.util;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

/** Permission nodes come from config.yml so they can be renamed. The admin node grants everything. */
public final class PermissionUtil {

    public enum Perm {
        USE("use", "ultrasbroadcast.use", PermissionDefault.OP),
        ADMIN("admin", "ultrasbroadcast.admin", PermissionDefault.OP),
        PUBLISH("publish", "ultrasbroadcast.publish", PermissionDefault.OP),
        CANCEL("cancel", "ultrasbroadcast.cancel", PermissionDefault.OP),
        LIST("list", "ultrasbroadcast.list", PermissionDefault.OP),
        DELETE("delete", "ultrasbroadcast.delete", PermissionDefault.OP),
        SETTINGS("settings", "ultrasbroadcast.settings", PermissionDefault.TRUE),
        RELOAD("reload", "ultrasbroadcast.reload", PermissionDefault.OP),
        BYPASS_COOLDOWN("bypass-cooldown", "ultrasbroadcast.bypass.cooldown", PermissionDefault.OP);

        private final String configKey;
        private final String defaultNode;
        private final PermissionDefault permissionDefault;

        Perm(String configKey, String defaultNode, PermissionDefault permissionDefault) {
            this.configKey = configKey;
            this.defaultNode = defaultNode;
            this.permissionDefault = permissionDefault;
        }
    }

    private final UltrasBroadcastPlugin plugin;
    private final Map<Perm, String> nodes = new EnumMap<>(Perm.class);

    public PermissionUtil(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    /** Reads the nodes from config.yml and registers the ones plugin.yml does not know about. */
    public void load() {
        PluginManager manager = plugin.getServer().getPluginManager();
        nodes.clear();
        for (Perm perm : Perm.values()) {
            String node = plugin.getConfig().getString("permissions." + perm.configKey, perm.defaultNode);
            if (node == null || node.isBlank()) {
                node = perm.defaultNode;
            }
            nodes.put(perm, node);
        }
        Permission admin = ensure(manager, Perm.ADMIN);
        for (Perm perm : Perm.values()) {
            if (perm == Perm.ADMIN) {
                continue;
            }
            Permission registered = ensure(manager, perm);
            if (admin != null && registered != null && !admin.getChildren().containsKey(registered.getName())) {
                registered.addParent(admin, true);
            }
        }
    }

    private Permission ensure(PluginManager manager, Perm perm) {
        String node = nodes.get(perm);
        Permission existing = manager.getPermission(node);
        if (existing != null) {
            return existing;
        }
        Permission created = new Permission(node, "ULTRAS_Broadcast_V1 - " + perm.configKey, perm.permissionDefault);
        manager.addPermission(created);
        return created;
    }

    public String node(Perm perm) {
        return nodes.getOrDefault(perm, perm.defaultNode);
    }

    public boolean has(CommandSender sender, Perm perm) {
        if (sender.hasPermission(node(perm))) {
            return true;
        }
        return perm != Perm.ADMIN && sender.hasPermission(node(Perm.ADMIN));
    }
}
