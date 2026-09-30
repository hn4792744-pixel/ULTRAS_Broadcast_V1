package me.uc_hussein.ultrasbroadcast.api;

import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import java.util.UUID;

/**
 * @param actorId      who publishes (Announcement.CONSOLE_UUID for the console)
 * @param type         display type
 * @param text         raw text, & color codes and &#RRGGBB are supported
 * @param durationMs   duration in milliseconds
 * @param targetPlayer null = everybody (global announcement), otherwise a personal announcement
 * @param targetName   name of the target, only used for messages and logs
 * @param skipCooldown true when the actor may ignore the cooldown
 */
public record PublishRequest(UUID actorId, String actorName, AnnouncementType type, String text, long durationMs,
                             UUID targetPlayer, String targetName, boolean skipCooldown) {
}
