package me.uc_hussein.ultrasbroadcast.service;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import me.uc_hussein.ultrasbroadcast.api.PublishRequest;
import me.uc_hussein.ultrasbroadcast.api.PublishResult;
import me.uc_hussein.ultrasbroadcast.api.PublishStatus;
import me.uc_hussein.ultrasbroadcast.config.PluginSettings;
import me.uc_hussein.ultrasbroadcast.event.AnnouncementCancelEvent;
import me.uc_hussein.ultrasbroadcast.event.AnnouncementDeleteEvent;
import me.uc_hussein.ultrasbroadcast.event.AnnouncementFinishEvent;
import me.uc_hussein.ultrasbroadcast.event.AnnouncementPublishEvent;
import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementStatus;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import me.uc_hussein.ultrasbroadcast.storage.AnnouncementRepository;
import me.uc_hussein.ultrasbroadcast.util.ColorUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Core of the plugin. Every state change runs on the main thread inside a synchronized method, so two administrators
 * can never create two global announcements. The single timer task computes remaining = endAt - now for everybody,
 * so all players see the same time. Database writes are queued on the storage thread.
 */
public final class AnnouncementService {

    public enum CancelStatus { CANCELLED, NONE, BLOCKED }

    public enum DeleteStatus { DELETED, NOT_FOUND, ACTIVE, BLOCKED }

    public record CancelResult(CancelStatus status, Announcement announcement) {
    }

    /** One running announcement (global or personal). */
    private static final class Display {
        final Announcement announcement;
        final AnnouncementType location;
        final Component text;
        final UUID personalTarget;
        final long endAt;
        final Set<UUID> seen = new HashSet<>();
        final Map<String, Component> chatCache = new HashMap<>();

        Display(Announcement announcement, AnnouncementType location, UUID personalTarget) {
            this.announcement = announcement;
            this.location = location;
            this.text = ColorUtil.parse(announcement.message());
            this.personalTarget = personalTarget;
            this.endAt = announcement.endAt();
        }

        boolean global() {
            return personalTarget == null;
        }
    }

    private final UltrasBroadcastPlugin plugin;
    private final AnnouncementRepository repository;
    private final Map<Long, Announcement> announcements = new LinkedHashMap<>();
    private final List<Display> displays = new ArrayList<>();
    private final Map<UUID, Long> lastPublish = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1L);
    private BukkitTask tickTask;
    private BukkitTask maintenanceTask;

    public AnnouncementService(UltrasBroadcastPlugin plugin, AnnouncementRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    // ------------------------------------------------------------------ life cycle

    public synchronized void load() throws SQLException {
        nextId.set(repository.maxId() + 1L);
        long now = System.currentTimeMillis();
        PluginSettings settings = plugin.settings();
        for (Announcement a : repository.loadAll()) {
            announcements.put(a.id(), a);
            if (a.status() != AnnouncementStatus.ACTIVE) {
                continue;
            }
            if (settings.restoreAfterRestart() && a.endAt() > now && a.isGlobal()) {
                displays.add(new Display(a, a.location(), null));
                plugin.audit().log("RESTORE", "Restored announcement #" + a.id() + " (" + (a.endAt() - now) + " ms left)");
            } else {
                a.setStatus(AnnouncementStatus.FINISHED);
                a.setEndedAt(Math.min(a.endAt(), now));
                persist(a);
            }
        }
        schedule();
        cleanup();
    }

    /** (Re)starts the timer with the interval from config.yml. */
    public synchronized void schedule() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (maintenanceTask != null) {
            maintenanceTask.cancel();
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, plugin.settings().updateIntervalTicks());
        maintenanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 20L * 60L, 20L * 3600L);
    }

    public synchronized void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (maintenanceTask != null) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
        long now = System.currentTimeMillis();
        boolean restore = plugin.settings().restoreAfterRestart();
        for (Display d : new ArrayList<>(displays)) {
            if (d.global() && restore) {
                continue;
            }
            end(d, AnnouncementStatus.FINISHED, now);
        }
    }

    private void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("ULTRAS_Broadcast_V1 must be used from the main server thread");
        }
    }

    // ------------------------------------------------------------------ queries

    public synchronized Announcement get(long id) {
        return announcements.get(id);
    }

    /** Non-deleted global announcements, newest first. */
    public synchronized List<Announcement> list() {
        List<Announcement> result = new ArrayList<>(announcements.values());
        result.sort(Comparator.comparingLong(Announcement::id).reversed());
        return result;
    }

    public synchronized boolean isGlobalActive() {
        return activeGlobal() != null;
    }

    public synchronized Announcement activeGlobalSnapshot() {
        Display d = activeGlobal();
        return d == null ? null : d.announcement.copy();
    }

    public synchronized long activeGlobalRemaining() {
        Display d = activeGlobal();
        return d == null ? 0L : Math.max(0L, d.endAt - System.currentTimeMillis());
    }

    private Display activeGlobal() {
        for (Display d : displays) {
            if (d.global()) {
                return d;
            }
        }
        return null;
    }

    private Display personalFor(UUID target) {
        for (Display d : displays) {
            if (target.equals(d.personalTarget)) {
                return d;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ publish

    public synchronized PublishResult publish(PublishRequest r) {
        requireMainThread();
        PluginSettings s = plugin.settings();
        if (!s.typeEnabled(r.type())) {
            return PublishResult.of(PublishStatus.TYPE_DISABLED);
        }
        if (ColorUtil.plain(r.text()).isBlank()) {
            return PublishResult.of(PublishStatus.EMPTY_TEXT);
        }
        if (r.durationMs() < s.minDurationMs() || r.durationMs() > s.maxDurationMs()) {
            return PublishResult.of(PublishStatus.INVALID_DURATION);
        }
        long now = System.currentTimeMillis();
        PublishResult cooldown = checkCooldown(r.actorId(), r.skipCooldown(), now);
        if (cooldown != null) {
            return cooldown;
        }
        boolean global = r.targetPlayer() == null;
        Display blocking = activeGlobal();
        if (global) {
            if (blocking != null && s.onlyOneGlobal()) {
                return alreadyActive(blocking, now);
            }
            PublishResult full = ensureCapacity();
            if (full != null) {
                return full;
            }
        } else {
            if (blocking != null && !s.personalWhileGlobal()) {
                return PublishResult.of(PublishStatus.PERSONAL_BLOCKED);
            }
            if (Bukkit.getPlayer(r.targetPlayer()) == null) {
                return PublishResult.of(PublishStatus.TARGET_OFFLINE);
            }
            if (personalFor(r.targetPlayer()) != null) {
                return PublishResult.of(PublishStatus.PLAYER_BUSY);
            }
        }
        long id = global ? nextId.getAndIncrement() : 0L;
        Announcement a = new Announcement(id, r.text(), r.type(), r.type(), r.actorId(), r.actorName(),
                global ? Announcement.TARGET_ALL : r.targetPlayer().toString(), now, now, 0L, now + r.durationMs(),
                r.durationMs(), AnnouncementStatus.ACTIVE, 0, 0L, false, 0L);
        AnnouncementPublishEvent event = new AnnouncementPublishEvent(a.copy(), r.type(), false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return PublishResult.of(PublishStatus.BLOCKED_BY_EVENT);
        }
        if (global) {
            announcements.put(id, a);
        }
        lastPublish.put(r.actorId(), now);
        Display display = new Display(a, r.type(), global ? null : r.targetPlayer());
        displays.add(display);
        render(display, now, true);
        if (global) {
            persist(a);
            plugin.audit().log("PUBLISH", r.actorName() + " published announcement #" + id + " ("
                    + r.type().key() + ", " + (r.durationMs() / 1000.0D) + "s)");
        } else {
            plugin.audit().log("PUBLISH", r.actorName() + " sent a personal " + r.type().key() + " announcement to "
                    + r.targetName() + " (" + (r.durationMs() / 1000.0D) + "s)");
        }
        return new PublishResult(PublishStatus.SUCCESS, a.copy(), 0L);
    }

    /** Publishes an existing announcement again, possibly in another location. */
    public synchronized PublishResult republish(UUID actorId, String actorName, long id, AnnouncementType location,
                                                boolean skipCooldown) {
        requireMainThread();
        PluginSettings s = plugin.settings();
        Announcement a = announcements.get(id);
        if (a == null || a.deleted()) {
            return PublishResult.of(PublishStatus.NOT_FOUND);
        }
        if (!s.typeEnabled(location)) {
            return PublishResult.of(PublishStatus.TYPE_DISABLED);
        }
        long now = System.currentTimeMillis();
        Display blocking = activeGlobal();
        if (a.status() == AnnouncementStatus.ACTIVE || (blocking != null && s.onlyOneGlobal())) {
            return alreadyActive(blocking != null ? blocking : displayOf(a), now);
        }
        PublishResult cooldown = checkCooldown(actorId, skipCooldown, now);
        if (cooldown != null) {
            return cooldown;
        }
        AnnouncementPublishEvent event = new AnnouncementPublishEvent(a.copy(), location, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return PublishResult.of(PublishStatus.BLOCKED_BY_EVENT);
        }
        a.setLocation(location);
        a.setStartedAt(now);
        a.setEndAt(now + a.durationMs());
        a.setEndedAt(0L);
        a.setStatus(AnnouncementStatus.ACTIVE);
        a.incrementRepublish();
        lastPublish.put(actorId, now);
        Display display = new Display(a, location, null);
        displays.add(display);
        render(display, now, true);
        persist(a);
        plugin.audit().log("REPUBLISH", actorName + " republished announcement #" + id + " in " + location.key());
        return new PublishResult(PublishStatus.SUCCESS, a.copy(), 0L);
    }

    private Display displayOf(Announcement a) {
        for (Display d : displays) {
            if (d.announcement == a) {
                return d;
            }
        }
        return null;
    }

    private PublishResult alreadyActive(Display blocking, long now) {
        long remaining = blocking == null ? 0L : Math.max(0L, blocking.endAt - now);
        return new PublishResult(PublishStatus.ALREADY_ACTIVE, null, remaining);
    }

    private PublishResult checkCooldown(UUID actor, boolean skip, long now) {
        int seconds = plugin.settings().cooldownSeconds();
        if (seconds <= 0 || skip || Announcement.CONSOLE_UUID.equals(actor)) {
            return null;
        }
        Long last = lastPublish.get(actor);
        if (last == null) {
            return null;
        }
        long left = last + seconds * 1000L - now;
        return left > 0L ? new PublishResult(PublishStatus.COOLDOWN, null, left) : null;
    }

    private PublishResult ensureCapacity() {
        PluginSettings s = plugin.settings();
        while (announcements.size() >= s.maxAnnouncements()) {
            Announcement oldest = null;
            for (Announcement candidate : announcements.values()) {
                if (candidate.status() != AnnouncementStatus.ACTIVE && (oldest == null || candidate.id() < oldest.id())) {
                    oldest = candidate;
                }
            }
            if (oldest == null || !s.deleteOldestWhenFull()) {
                return PublishResult.of(PublishStatus.LIST_FULL);
            }
            softDelete(oldest, System.currentTimeMillis());
            plugin.audit().log("AUTO-DELETE", "List full, deleted the oldest announcement #" + oldest.id());
        }
        return null;
    }

    // ------------------------------------------------------------------ cancel

    /** Cancels the active global announcement. */
    public synchronized CancelResult cancelGlobal(String actorName) {
        requireMainThread();
        Display d = activeGlobal();
        if (d == null) {
            return new CancelResult(CancelStatus.NONE, null);
        }
        return cancel(d, actorName);
    }

    /** Cancels the personal announcement of one player. */
    public synchronized CancelResult cancelPersonal(UUID target, String actorName) {
        requireMainThread();
        Display d = personalFor(target);
        if (d == null) {
            return new CancelResult(CancelStatus.NONE, null);
        }
        return cancel(d, actorName);
    }

    /** Cancels every running personal announcement. @return how many were cancelled */
    public synchronized int cancelAllPersonal(String actorName) {
        requireMainThread();
        int count = 0;
        for (Display d : new ArrayList<>(displays)) {
            if (!d.global() && cancel(d, actorName).status() == CancelStatus.CANCELLED) {
                count++;
            }
        }
        return count;
    }

    private CancelResult cancel(Display d, String actorName) {
        AnnouncementCancelEvent event = new AnnouncementCancelEvent(d.announcement.copy(), actorName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return new CancelResult(CancelStatus.BLOCKED, null);
        }
        end(d, AnnouncementStatus.CANCELLED, System.currentTimeMillis());
        if (d.global()) {
            plugin.audit().log("CANCEL", actorName + " cancelled announcement #" + d.announcement.id());
        } else {
            plugin.audit().log("CANCEL", actorName + " cancelled a personal announcement");
        }
        return new CancelResult(CancelStatus.CANCELLED, d.announcement.copy());
    }

    // ------------------------------------------------------------------ delete

    public synchronized DeleteStatus delete(String actorName, long id) {
        requireMainThread();
        Announcement a = announcements.get(id);
        if (a == null) {
            return DeleteStatus.NOT_FOUND;
        }
        if (a.status() == AnnouncementStatus.ACTIVE) {
            return DeleteStatus.ACTIVE;
        }
        AnnouncementDeleteEvent event = new AnnouncementDeleteEvent(a.copy(), actorName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return DeleteStatus.BLOCKED;
        }
        softDelete(a, System.currentTimeMillis());
        plugin.audit().log("DELETE", actorName + " deleted announcement #" + id);
        return DeleteStatus.DELETED;
    }

    /** Deletes every finished announcement (and the active one when delete-all.cancel-active is true). */
    public synchronized int deleteAll(String actorName) {
        requireMainThread();
        if (plugin.settings().deleteAllCancelsActive()) {
            cancelGlobal(actorName);
        }
        int count = 0;
        for (Announcement a : new ArrayList<>(announcements.values())) {
            if (delete(actorName, a.id()) == DeleteStatus.DELETED) {
                count++;
            }
        }
        plugin.audit().log("DELETE-ALL", actorName + " deleted " + count + " announcements");
        return count;
    }

    private void softDelete(Announcement a, long now) {
        a.markDeleted(now);
        announcements.remove(a.id());
        persist(a);
    }

    // ------------------------------------------------------------------ timer / rendering

    public synchronized void tick() {
        if (displays.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Display d : new ArrayList<>(displays)) {
            try {
                if (now >= d.endAt) {
                    finish(d);
                } else {
                    render(d, now, false);
                }
            } catch (RuntimeException ex) {
                plugin.audit().error("TIMER", "Error while updating an announcement", ex);
            }
        }
    }

    private void finish(Display d) {
        end(d, AnnouncementStatus.FINISHED, d.endAt);
        Bukkit.getPluginManager().callEvent(new AnnouncementFinishEvent(d.announcement.copy()));
        if (d.global()) {
            plugin.audit().log("FINISH", "Announcement #" + d.announcement.id() + " finished");
        }
    }

    private void end(Display d, AnnouncementStatus status, long when) {
        displays.remove(d);
        Announcement a = d.announcement;
        a.setStatus(status);
        a.setEndedAt(when);
        for (UUID id : d.seen) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                clear(d, player);
            }
        }
        d.seen.clear();
        if (d.global()) {
            persist(a);
        }
    }

    /**
     * Sends what every eligible player must see right now. A player who turned the type off gets it cleared,
     * a player who turned it on (or joined) starts seeing it, all from the same endAt timestamp.
     */
    private void render(Display d, long now, boolean initial) {
        long remaining = Math.max(0L, d.endAt - now);
        Component bar = switch (d.location) {
            case HEARTS -> plugin.renderer().heartsBar(d.text, remaining);
            case SCREEN -> plugin.renderer().timer(remaining);
            case CHAT -> null;
        };
        if (d.global()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                renderFor(d, player, remaining, bar, initial);
            }
        } else {
            Player target = Bukkit.getPlayer(d.personalTarget);
            if (target != null) {
                renderFor(d, target, remaining, bar, initial);
            }
        }
    }

    private void renderFor(Display d, Player player, long remaining, Component bar, boolean initial) {
        UUID id = player.getUniqueId();
        boolean eligible = plugin.playerSettings().get(id).isEnabled(d.location);
        boolean seen = d.seen.contains(id);
        if (eligible && !seen) {
            d.seen.add(id);
            d.announcement.addViews(1L);
            switch (d.location) {
                case CHAT -> player.sendMessage(d.chatCache.computeIfAbsent(plugin.messages().langOf(player),
                        lang -> plugin.renderer().chatBlock(lang, d.text)));
                case SCREEN -> player.showTitle(plugin.renderer().screenTitle(d.text, remaining, initial));
                case HEARTS -> { }
            }
            if (initial) {
                plugin.sounds().playAnnouncement(player);
            }
        } else if (!eligible && seen) {
            d.seen.remove(id);
            clear(d, player);
            return;
        }
        if (eligible && bar != null) {
            player.sendActionBar(bar);
        }
    }

    private void clear(Display d, Player player) {
        switch (d.location) {
            case SCREEN -> {
                player.clearTitle();
                player.sendActionBar(Component.empty());
            }
            case HEARTS -> player.sendActionBar(Component.empty());
            case CHAT -> { }
        }
    }

    public synchronized void handleQuit(UUID uuid) {
        long now = System.currentTimeMillis();
        for (Display d : new ArrayList<>(displays)) {
            d.seen.remove(uuid);
            if (uuid.equals(d.personalTarget)) {
                end(d, AnnouncementStatus.FINISHED, now);
            }
        }
        lastPublish.remove(uuid);
    }

    // ------------------------------------------------------------------ storage

    private void persist(Announcement a) {
        repository.save(a.copy());
    }

    /** Auto-delete of old announcements and hard purge of old deleted records. */
    public synchronized void cleanup() {
        PluginSettings s = plugin.settings();
        long now = System.currentTimeMillis();
        if (s.autoDeleteEnabled()) {
            long cutoff = now - s.autoDeleteDays() * 86_400_000L;
            int count = 0;
            for (Announcement a : new ArrayList<>(announcements.values())) {
                long reference = a.endedAt() > 0L ? a.endedAt() : a.createdAt();
                if (a.status() != AnnouncementStatus.ACTIVE && reference < cutoff) {
                    softDelete(a, now);
                    count++;
                }
            }
            if (count > 0) {
                plugin.audit().log("AUTO-DELETE", "Removed " + count + " announcements older than " + s.autoDeleteDays() + " days");
            }
        }
        if (s.purgeDeletedDays() > 0) {
            repository.purgeDeletedBefore(now - s.purgeDeletedDays() * 86_400_000L);
        }
    }
}
