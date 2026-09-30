package me.uc_hussein.ultrasbroadcast.api;

import me.uc_hussein.ultrasbroadcast.model.Announcement;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Public API. All methods must be called from the main server thread.
 * <pre>BroadcastAPI api = BroadcastAPI.get();</pre>
 */
public interface BroadcastAPI {

    /** @return the API, or null when ULTRAS_Broadcast_V1 is not enabled */
    static BroadcastAPI get() {
        RegisteredServiceProvider<BroadcastAPI> provider = Bukkit.getServicesManager().getRegistration(BroadcastAPI.class);
        return provider == null ? null : provider.getProvider();
    }

    /** Publishes an announcement with exactly the same rules as /bc. */
    PublishResult publish(PublishRequest request);

    /** Cancels the active global announcement.
     * @return true when something was cancelled */
    boolean cancel(String actorName);

    /** @return true when a global announcement is active */
    boolean isActive();

    /** @return a snapshot of the active global announcement */
    Optional<Announcement> getActiveAnnouncement();
}
