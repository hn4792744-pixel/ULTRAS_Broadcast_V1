package me.uc_hussein.ultrasbroadcast.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component mini(String text, TagResolver... resolvers) {
        return MM.deserialize(text, TagResolver.resolver(resolvers));
    }

    public static String escape(String text) {
        return MM.escapeTags(text);
    }

    /** Items show custom names in italic unless it is switched off. */
    public static Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
