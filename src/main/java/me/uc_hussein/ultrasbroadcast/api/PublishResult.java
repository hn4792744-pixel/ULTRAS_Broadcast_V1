package me.uc_hussein.ultrasbroadcast.api;

import me.uc_hussein.ultrasbroadcast.model.Announcement;

/** @param remainingMs time left of the blocking announcement (ALREADY_ACTIVE) or cooldown left (COOLDOWN) */
public record PublishResult(PublishStatus status, Announcement announcement, long remainingMs) {
    public static PublishResult of(PublishStatus status) {
        return new PublishResult(status, null, 0L);
    }

    public boolean success() {
        return status == PublishStatus.SUCCESS;
    }
}
