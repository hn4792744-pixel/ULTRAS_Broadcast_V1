package me.uc_hussein.ultrasbroadcast.gui;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** One gui/&lt;name&gt;.yml: title, rows, filler, content slots and item definitions with variants. */
public final class GuiConfig {
    private static final Set<String> VARIANT_KEYS = Set.of("enabled", "disabled", "active", "finished", "selected", "unselected");

    /** Everything editable about one item. Variants inherit the fields they do not override. */
    public record ItemDef(int[] slots, Material material, String name, List<String> lore, boolean glow, int amount,
                          String sound, boolean visible, Map<String, ItemDef> variants) {
        public ItemDef variant(String key) {
            return key == null ? this : variants.getOrDefault(key, this);
        }
    }

    private final String name;
    private final int rows;
    private final String title;
    private final ItemDef filler;
    private final int[] contentSlots;
    private final Map<String, ItemDef> items;

    private GuiConfig(String name, int rows, String title, ItemDef filler, int[] contentSlots, Map<String, ItemDef> items) {
        this.name = name;
        this.rows = rows;
        this.title = title;
        this.filler = filler;
        this.contentSlots = contentSlots;
        this.items = items;
    }

    public String name() { return name; }
    public int rows() { return rows; }
    public String title() { return title; }
    /** null when the filler is disabled */
    public ItemDef filler() { return filler; }
    public int[] contentSlots() { return contentSlots; }
    public ItemDef item(String key) { return items.get(key); }

    public static GuiConfig load(UltrasBroadcastPlugin plugin, String name) {
        String resource = "gui/" + name + ".yml";
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isInt("rows") || !yaml.isConfigurationSection("items")) {
            plugin.audit().warn("CONFIG", resource + " is invalid, using the bundled version");
            yaml = bundled(plugin, resource);
        }
        return parse(plugin, name, yaml);
    }

    private static YamlConfiguration bundled(UltrasBroadcastPlugin plugin, String resource) {
        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (java.io.IOException ex) {
            plugin.audit().error("CONFIG", "Could not read bundled " + resource, ex);
        }
        return new YamlConfiguration();
    }

    private static GuiConfig parse(UltrasBroadcastPlugin plugin, String name, YamlConfiguration yaml) {
        int rows = Math.max(1, Math.min(6, yaml.getInt("rows", 3)));
        String title = yaml.getString("title", "ULTRAS");
        ItemDef filler = null;
        ConfigurationSection fillerSection = yaml.getConfigurationSection("filler");
        if (fillerSection != null && fillerSection.getBoolean("enabled", true)) {
            filler = parseItem(plugin, name, "filler", fillerSection, null);
        }
        int[] content = parseSlots(yaml.getList("content-slots"), rows * 9);
        Map<String, ItemDef> items = new HashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection itemSection = section.getConfigurationSection(key);
                if (itemSection != null) {
                    items.put(key, parseItem(plugin, name, key, itemSection, null));
                }
            }
        }
        return new GuiConfig(name, rows, title, filler, content, items);
    }

    private static ItemDef parseItem(UltrasBroadcastPlugin plugin, String gui, String key, ConfigurationSection s, ItemDef base) {
        int[] slots = base == null ? new int[0] : base.slots();
        if (s.contains("slot")) {
            slots = new int[] {s.getInt("slot")};
        } else if (s.contains("slots")) {
            slots = parseSlots(s.getList("slots"), 54);
        }
        Material material = base == null ? Material.STONE : base.material();
        if (s.contains("material")) {
            Material matched = Material.matchMaterial(s.getString("material", "STONE"));
            if (matched == null || matched.isAir()) {
                plugin.audit().warn("CONFIG", "gui/" + gui + ".yml: unknown material '" + s.getString("material") + "' in " + key);
            } else {
                material = matched;
            }
        }
        String itemName = s.contains("name") ? s.getString("name") : base == null ? null : base.name();
        List<String> lore = base == null ? List.of() : base.lore();
        if (s.isList("lore")) {
            lore = s.getStringList("lore");
        } else if (s.isString("lore")) {
            lore = List.of(s.getString("lore", ""));
        }
        boolean glow = s.contains("glow") ? s.getBoolean("glow") : base != null && base.glow();
        int amount = s.contains("amount") ? Math.max(1, Math.min(64, s.getInt("amount"))) : base == null ? 1 : base.amount();
        String sound = s.contains("sound") ? s.getString("sound") : base == null ? null : base.sound();
        boolean visible = s.contains("visible") ? s.getBoolean("visible") : base == null || base.visible();

        Map<String, ItemDef> variants = new HashMap<>();
        ItemDef def = new ItemDef(slots, material, itemName, lore, glow, amount, sound, visible, variants);
        for (String variantKey : s.getKeys(false)) {
            ConfigurationSection variant = s.getConfigurationSection(variantKey);
            if (variant != null && VARIANT_KEYS.contains(variantKey)) {
                variants.put(variantKey, parseItem(plugin, gui, key + "." + variantKey, variant, def));
            }
        }
        return def;
    }

    /** Accepts numbers and ranges such as "10-16". */
    private static int[] parseSlots(List<?> raw, int max) {
        List<Integer> result = new ArrayList<>();
        if (raw != null) {
            for (Object entry : raw) {
                String text = String.valueOf(entry).trim();
                try {
                    if (text.contains("-")) {
                        String[] parts = text.split("-", 2);
                        int from = Integer.parseInt(parts[0].trim());
                        int to = Integer.parseInt(parts[1].trim());
                        for (int i = from; i <= to; i++) {
                            if (i >= 0 && i < max) {
                                result.add(i);
                            }
                        }
                    } else {
                        int slot = Integer.parseInt(text);
                        if (slot >= 0 && slot < max) {
                            result.add(slot);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // invalid entries are skipped
                }
            }
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
}
