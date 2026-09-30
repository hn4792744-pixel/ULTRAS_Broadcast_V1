package me.uc_hussein.ultrasbroadcast.api;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.service.AnnouncementService;
import java.util.Optional;

public final class BroadcastAPIImpl implements BroadcastAPI {
    private final UltrasBroadcastPlugin plugin;

    public BroadcastAPIImpl(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PublishResult publish(PublishRequest request) {
        return plugin.announcements().publish(request);
    }

    @Override
    public boolean cancel(String actorName) {
        return plugin.announcements().cancelGlobal(actorName).status() == AnnouncementService.CancelStatus.CANCELLED;
    }

    @Override
    public boolean isActive() {
        return plugin.announcements().isGlobalActive();
    }

    @Override
    public Optional<Announcement> getActiveAnnouncement() {
        return Optional.ofNullable(plugin.announcements().activeGlobalSnapshot());
    }
}
