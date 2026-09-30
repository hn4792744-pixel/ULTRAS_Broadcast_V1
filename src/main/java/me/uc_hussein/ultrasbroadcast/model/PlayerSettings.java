package me.uc_hussein.ultrasbroadcast.model;

import java.util.UUID;

/** Per-player preferences. */
public final class PlayerSettings {
    private final UUID uuid;
    private volatile String name;
    private volatile String language;
    private volatile boolean sounds;
    private volatile boolean chat;
    private volatile boolean screen;
    private volatile boolean hearts;
    private volatile long updatedAt;

    public PlayerSettings(UUID uuid, String name, String language, boolean sounds, boolean chat, boolean screen,
                          boolean hearts, long updatedAt) {
        this.uuid = uuid;
        this.name = name;
        this.language = language;
        this.sounds = sounds;
        this.chat = chat;
        this.screen = screen;
        this.hearts = hearts;
        this.updatedAt = updatedAt;
    }

    public PlayerSettings copy() {
        return new PlayerSettings(uuid, name, language, sounds, chat, screen, hearts, updatedAt);
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public String language() { return language; }
    public boolean sounds() { return sounds; }
    public boolean chat() { return chat; }
    public boolean screen() { return screen; }
    public boolean hearts() { return hearts; }
    public long updatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setLanguage(String language) { this.language = language; }
    public void touch() { this.updatedAt = System.currentTimeMillis(); }

    public boolean isEnabled(AnnouncementType type) {
        return switch (type) {
            case CHAT -> chat;
            case SCREEN -> screen;
            case HEARTS -> hearts;
        };
    }

    /** Toggle keys: sounds, chat, screen, hearts. */
    public boolean get(String key) {
        return switch (key) {
            case "sounds" -> sounds;
            case "chat" -> chat;
            case "screen" -> screen;
            case "hearts" -> hearts;
            default -> false;
        };
    }

    public void set(String key, boolean value) {
        switch (key) {
            case "sounds" -> sounds = value;
            case "chat" -> chat = value;
            case "screen" -> screen = value;
            case "hearts" -> hearts = value;
            default -> { }
        }
    }
}
