package me.uc_hussein.ultrasbroadcast.event;

import me.uc_hussein.ultrasbroadcast.model.Announcement;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired on the main thread. */
public class AnnouncementCancelEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Announcement announcement;
    private final String actorName;
    public String getActorName() { return actorName; }

    private boolean cancelled;

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    public AnnouncementCancelEvent(Announcement announcement, String actorName) {
        this.announcement = announcement;
        this.actorName = actorName;
    }

    /** Snapshot of the announcement. */
    public Announcement getAnnouncement() { return announcement; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
