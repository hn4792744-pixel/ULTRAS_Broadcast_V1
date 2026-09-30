package me.uc_hussein.ultrasbroadcast.logging;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Console + logs/YYYY-MM-DD.log. File writes happen on a dedicated thread. */
public final class AuditLogger {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UltrasBroadcastPlugin plugin;
    private final File folder;
    private ExecutorService executor;
    private boolean enabled;
    private boolean console;
    private boolean file;

    public AuditLogger(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "logs");
    }

    public synchronized void start() {
        configure();
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "ULTRAS_Broadcast-Log");
                thread.setDaemon(true);
                return thread;
            });
        }
        if (enabled && file) {
            cleanOldLogs();
        }
    }

    /** Re-reads logging.* from config.yml. */
    public synchronized void configure() {
        enabled = plugin.getConfig().getBoolean("logging.enabled", true);
        console = plugin.getConfig().getBoolean("logging.console", true);
        file = plugin.getConfig().getBoolean("logging.file", true);
    }

    public void log(String category, String message) {
        write(Level.INFO, category, message, null);
    }

    public void warn(String category, String message) {
        write(Level.WARNING, category, message, null);
    }

    public void error(String category, String message, Throwable error) {
        write(Level.SEVERE, category, message, error);
    }

    private synchronized void write(Level level, String category, String message, Throwable error) {
        if (!enabled && level.intValue() < Level.WARNING.intValue()) {
            return;
        }
        String line = "[" + category + "] " + message;
        if (console || level.intValue() >= Level.WARNING.intValue()) {
            if (error != null) {
                plugin.getLogger().log(level, line, error);
            } else {
                plugin.getLogger().log(level, line);
            }
        }
        if (file && executor != null && !executor.isShutdown()) {
            String stamp = LocalDateTime.now().format(STAMP);
            String text = stamp + " " + line + (error == null ? "" : " :: " + error) + System.lineSeparator();
            String name = LocalDate.now() + ".log";
            executor.execute(() -> append(name, text));
        }
    }

    private void append(String fileName, String text) {
        try {
            Files.createDirectories(folder.toPath());
            Files.writeString(new File(folder, fileName).toPath(), text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not write log file", ex);
        }
    }

    private void cleanOldLogs() {
        int keep = plugin.getConfig().getInt("logging.keep-days", 30);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".log"));
        if (keep <= 0 || files == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - keep * 86_400_000L;
        for (File logFile : files) {
            if (logFile.lastModified() < cutoff && !logFile.delete()) {
                plugin.getLogger().warning("Could not delete old log " + logFile.getName());
            }
        }
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
    }
}
