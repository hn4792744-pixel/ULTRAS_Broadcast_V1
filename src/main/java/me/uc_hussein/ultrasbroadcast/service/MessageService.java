package me.uc_hussein.ultrasbroadcast.service;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementStatus;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import me.uc_hussein.ultrasbroadcast.util.GradientUtil;
import me.uc_hussein.ultrasbroadcast.util.Text;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * All text comes from messages_&lt;lang&gt;.yml. Chat messages look like: PREFIX symbol body.
 * MiniMessage tags available everywhere: &lt;accent&gt; &lt;text&gt; &lt;ok&gt; &lt;warn&gt; &lt;err&gt; &lt;player&gt;
 * &lt;brand&gt; &lt;prefix&gt;.
 */
public final class MessageService {

    public enum Level { SUCCESS, WARNING, ERROR, INFO, ACTION }

    private final UltrasBroadcastPlugin plugin;
    private final Function<UUID, String> languageOf;
    private final Map<String, YamlConfiguration> files = new HashMap<>();
    private final Map<String, YamlConfiguration> bundled = new HashMap<>();
    private TagResolver base = TagResolver.empty();
    private Component brand = Component.empty();
    private Component prefix = Component.empty();

    public MessageService(UltrasBroadcastPlugin plugin, Function<UUID, String> languageOf) {
        this.plugin = plugin;
        this.languageOf = languageOf;
    }

    public void load() {
        PluginSettings s = plugin.settings();
        files.clear();
        bundled.clear();
        for (String code : s.languages()) {
            String name = "messages_" + code + ".yml";
            File file = new File(plugin.getDataFolder(), name);
            if (!file.exists() && plugin.getResource(name) != null) {
                plugin.saveResource(name, false);
            }
            YamlConfiguration jar = readBundled(name);
            if (jar != null) {
                bundled.put(code, jar);
            }
            if (file.exists()) {
                try {
                    files.put(code, YamlConfiguration.loadConfiguration(file));
                } catch (RuntimeException ex) {
                    plugin.audit().error("CONFIG", "Could not read " + name, ex);
                }
            } else {
                plugin.audit().warn("CONFIG", name + " is missing; falling back to the default language");
            }
        }
        brand = GradientUtil.gradient(s.brandName(), s.gradientEnabled(), s.gradientStart(), s.gradientEnd());
        prefix = Component.empty().append(brand).append(Component.space())
                .append(Component.text(s.separator(), s.color("separator")));
        base = TagResolver.resolver(
                TagResolver.resolver("accent", Tag.styling(s.color("accent"))),
                TagResolver.resolver("text", Tag.styling(s.color("text"))),
                TagResolver.resolver("ok", Tag.styling(s.color("success"))),
                TagResolver.resolver("warn", Tag.styling(s.color("warning"))),
                TagResolver.resolver("err", Tag.styling(s.color("error"))),
                Placeholder.component("brand", brand),
                Placeholder.component("prefix", prefix));
    }

    private YamlConfiguration readBundled(String name) {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ language

    public String langOf(CommandSender sender) {
        if (sender instanceof Player player) {
            return languageOf.apply(player.getUniqueId());
        }
        return plugin.settings().defaultLanguage();
    }

    public String languageName(String code) {
        String value = lookup(code, "language-name");
        return value == null ? code : value;
    }

    // ------------------------------------------------------------------ raw access

    private String lookup(String lang, String key) {
        YamlConfiguration file = files.get(lang);
        if (file != null && file.isString(key)) {
            return file.getString(key);
        }
        YamlConfiguration jar = bundled.get(lang);
        if (jar != null && jar.isString(key)) {
            return jar.getString(key);
        }
        return null;
    }

    private List<String> lookupList(String lang, String key) {
        YamlConfiguration file = files.get(lang);
        if (file != null && file.isList(key)) {
            return file.getStringList(key);
        }
        YamlConfiguration jar = bundled.get(lang);
        if (jar != null && jar.isList(key)) {
            return jar.getStringList(key);
        }
        return null;
    }

    public String raw(String lang, String key) {
        String value = lookup(lang, key);
        if (value == null) {
            value = lookup(plugin.settings().defaultLanguage(), key);
        }
        if (value == null) {
            value = lookup("en", key);
        }
        if (value == null) {
            plugin.audit().warn("CONFIG", "Missing message key '" + key + "'");
            return "<err>" + key;
        }
        return value;
    }

    public List<String> rawList(String lang, String key) {
        List<String> value = lookupList(lang, key);
        if (value == null) {
            value = lookupList(plugin.settings().defaultLanguage(), key);
        }
        if (value == null) {
            value = lookupList("en", key);
        }
        if (value == null) {
            plugin.audit().warn("CONFIG", "Missing message list '" + key + "'");
            return List.of("<err>" + key);
        }
        return value;
    }

    // ------------------------------------------------------------------ building

    public Component brand() { return brand; }
    public Component prefix() { return prefix; }

    public Component mini(String text, TagResolver... tags) {
        TagResolver[] all = new TagResolver[tags.length + 1];
        all[0] = base;
        System.arraycopy(tags, 0, all, 1, tags.length);
        return Text.mini(text, all);
    }

    /** A message key rendered without prefix (used by GUI and state texts). */
    public Component component(String lang, String key, TagResolver... tags) {
        return Text.noItalic(mini(raw(lang, key), tags));
    }

    public Component build(String lang, Level level, String key, TagResolver... tags) {
        PluginSettings s = plugin.settings();
        String colorKey = switch (level) {
            case SUCCESS -> "success";
            case WARNING -> "warning";
            case ERROR -> "error";
            case INFO, ACTION -> "info";
        };
        String symbolKey = level.name().toLowerCase(java.util.Locale.ROOT);
        Component body = mini(raw(lang, key), tags).colorIfAbsent(s.color("text"));
        return Component.empty()
                .append(prefix).append(Component.space())
                .append(Component.text(s.symbol(symbolKey), s.color(colorKey)))
                .append(Component.space())
                .append(body);
    }

    public void send(CommandSender sender, Level level, String key, TagResolver... tags) {
        sender.sendMessage(build(langOf(sender), level, key, tags));
    }

    /** <player> placeholder rendered in the configured player color. */
    public TagResolver player(String name) {
        return Placeholder.component("player", Component.text(name, plugin.settings().color("player")));
    }

    // ------------------------------------------------------------------ GUI text

    /** Resolves "lang:some.key" references, otherwise treats the value as literal MiniMessage. */
    public Component resolveText(String lang, String value, TagResolver... tags) {
        String text = value.startsWith("lang:") ? raw(lang, value.substring(5)) : value;
        return Text.noItalic(mini(text, tags));
    }

    /** Lines may be literal, "lang:key" (string or list) and may start with [active] / [finished]. */
    public List<Component> resolveLore(String lang, List<String> lines, Set<String> flags, TagResolver... tags) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) {
            List<String> expanded = new ArrayList<>();
            if (line.startsWith("lang:")) {
                String key = line.substring(5);
                List<String> list = lookupList(lang, key);
                if (list == null && lookup(lang, key) == null) {
                    list = rawList(lang, key);
                }
                if (list != null) {
                    expanded.addAll(list);
                } else {
                    expanded.add(raw(lang, key));
                }
            } else {
                expanded.add(line);
            }
            for (String text : expanded) {
                String content = text;
                if (content.startsWith("[active]") || content.startsWith("[finished]")) {
                    int end = content.indexOf(']');
                    String flag = content.substring(1, end);
                    if (!flags.contains(flag)) {
                        continue;
                    }
                    content = content.substring(end + 1);
                }
                result.add(content.isEmpty() ? Component.empty() : Text.noItalic(mini(content, tags)));
            }
        }
        return result;
    }

    public Component state(String lang, boolean enabled) {
        return component(lang, enabled ? "state-enabled" : "state-disabled");
    }

    public Component typeName(String lang, AnnouncementType type) {
        return component(lang, "type." + type.key());
    }

    public Component status(String lang, AnnouncementStatus status) {
        return component(lang, "status." + status.name().toLowerCase(java.util.Locale.ROOT));
    }
}
