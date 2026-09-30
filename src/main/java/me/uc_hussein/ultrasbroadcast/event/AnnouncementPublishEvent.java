package me.uc_hussein.ultrasbroadcast.event;

import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired on the main thread. */
public class AnnouncementPublishEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Announcement announcement;
    private final AnnouncementType location;
    private final boolean republish;
    /** Where it will be displayed. */
    public AnnouncementType getLocation() { return location; }
    /** true when an existing announcement is being published again. */
    public boolean isRepublish() { return republish; }

    private boolean cancelled;

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    public AnnouncementPublishEvent(Announcement announcement, AnnouncementType location, boolean republish) {
        this.announcement = announcement;
        this.location = location;
        this.republish = republish;
    }

    /** Snapshot of the announcement. */
    public Announcement getAnnouncement() { return announcement; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
