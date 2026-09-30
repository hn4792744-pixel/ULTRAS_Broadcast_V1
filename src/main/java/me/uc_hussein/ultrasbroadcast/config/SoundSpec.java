package me.uc_hussein.ultrasbroadcast.config;

/** A configured sound: name (ENUM_STYLE or namespaced.key), volume and pitch. */
public record SoundSpec(boolean enabled, String sound, float volume, float pitch) {
}
