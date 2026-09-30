package me.uc_hussein.ultrasbroadcast.event;

import me.uc_hussein.ultrasbroadcast.model.Announcement;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired on the main thread. */
public class AnnouncementFinishEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Announcement announcement;

    public AnnouncementFinishEvent(Announcement announcement) {
        this.announcement = announcement;
    }

    /** Snapshot of the announcement. */
    public Announcement getAnnouncement() { return announcement; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
