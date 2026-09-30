package me.uc_hussein.ultrasbroadcast.model;

import java.util.Locale;

/** Where an announcement is displayed. */
public enum AnnouncementType {
    CHAT, SCREEN, HEARTS;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
