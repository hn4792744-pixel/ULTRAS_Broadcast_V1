package me.uc_hussein.ultrasbroadcast.model;

import java.util.UUID;

/** One announcement. Instances returned by the public API are snapshots. */
public final class Announcement {
    public static final String TARGET_ALL = "ALL";
    public static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    private final long id;
    private final String message;
    private final AnnouncementType type;
    private AnnouncementType location;
    private final UUID creatorUuid;
    private final String creatorName;
    private final String target;
    private final long createdAt;
    private long startedAt;
    private long endedAt;
    private long endAt;
    private final long durationMs;
    private AnnouncementStatus status;
    private int republishCount;
    private long views;
    private boolean deleted;
    private long deletedAt;

    public Announcement(long id, String message, AnnouncementType type, AnnouncementType location, UUID creatorUuid,
                        String creatorName, String target, long createdAt, long startedAt, long endedAt, long endAt,
                        long durationMs, AnnouncementStatus status, int republishCount, long views, boolean deleted,
                        long deletedAt) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.location = location;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.target = target;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endAt = endAt;
        this.durationMs = durationMs;
        this.status = status;
        this.republishCount = republishCount;
        this.views = views;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    public Announcement copy() {
        return new Announcement(id, message, type, location, creatorUuid, creatorName, target, createdAt, startedAt,
                endedAt, endAt, durationMs, status, republishCount, views, deleted, deletedAt);
    }

    public long id() { return id; }
    /** Raw text as typed, including & color codes. */
    public String message() { return message; }
    /** Original type chosen when it was created. */
    public AnnouncementType type() { return type; }
    /** Where it is (or was last) displayed. */
    public AnnouncementType location() { return location; }
    public UUID creatorUuid() { return creatorUuid; }
    public String creatorName() { return creatorName; }
    public String target() { return target; }
    public boolean isGlobal() { return TARGET_ALL.equals(target); }
    public long createdAt() { return createdAt; }
    public long startedAt() { return startedAt; }
    public long endedAt() { return endedAt; }
    public long endAt() { return endAt; }
    public long durationMs() { return durationMs; }
    public AnnouncementStatus status() { return status; }
    public int republishCount() { return republishCount; }
    public long views() { return views; }
    public boolean deleted() { return deleted; }
    public long deletedAt() { return deletedAt; }
    public long remainingMs(long now) { return Math.max(0L, endAt - now); }

    public void setLocation(AnnouncementType location) { this.location = location; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public void setEndedAt(long endedAt) { this.endedAt = endedAt; }
    public void setEndAt(long endAt) { this.endAt = endAt; }
    public void setStatus(AnnouncementStatus status) { this.status = status; }
    public void incrementRepublish() { this.republishCount++; }
    public void addViews(long amount) { this.views += amount; }
    public void markDeleted(long now) { this.deleted = true; this.deletedAt = now; }
}
