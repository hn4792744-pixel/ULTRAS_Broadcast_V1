package me.uc_hussein.ultrasbroadcast.storage;

import me.uc_hussein.ultrasbroadcast.UltrasBroadcastPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * SQLite storage. A single dedicated thread owns the connection, so statements never run concurrently and
 * nothing heavy runs on the main thread after start-up.
 */
public final class DatabaseManager {

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlAction {
        void run(Connection connection) throws SQLException;
    }

    private final UltrasBroadcastPlugin plugin;
    private ExecutorService executor;
    private Connection connection;
    private volatile boolean closed = true;

    public DatabaseManager(UltrasBroadcastPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(File file) throws SQLException, ClassNotFoundException {
        // sqlite-jdbc is bundled with Paper.
        Class.forName("org.sqlite.JDBC");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Could not create " + parent);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("CREATE TABLE IF NOT EXISTS announcements ("
                    + "id INTEGER PRIMARY KEY,"
                    + "message TEXT NOT NULL,"
                    + "type TEXT NOT NULL,"
                    + "location TEXT NOT NULL,"
                    + "creator_uuid TEXT NOT NULL,"
                    + "creator_name TEXT NOT NULL,"
                    + "target TEXT NOT NULL,"
                    + "created_at INTEGER NOT NULL,"
                    + "started_at INTEGER NOT NULL,"
                    + "ended_at INTEGER NOT NULL,"
                    + "end_at INTEGER NOT NULL,"
                    + "duration_ms INTEGER NOT NULL,"
                    + "status TEXT NOT NULL,"
                    + "republish_count INTEGER NOT NULL,"
                    + "views INTEGER NOT NULL,"
                    + "deleted INTEGER NOT NULL,"
                    + "deleted_at INTEGER NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS player_settings ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "language TEXT NOT NULL,"
                    + "sounds INTEGER NOT NULL,"
                    + "chat INTEGER NOT NULL,"
                    + "screen INTEGER NOT NULL,"
                    + "hearts INTEGER NOT NULL,"
                    + "updated_at INTEGER NOT NULL)");
        }
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ULTRAS_Broadcast-DB");
            thread.setDaemon(true);
            return thread;
        });
        closed = false;
    }

    /** Runs a query on the database thread and waits for the result. Do not use on the main thread after start-up. */
    public <T> T call(SqlFunction<T> function) throws SQLException {
        if (closed) {
            throw new SQLException("Database is closed");
        }
        try {
            return executor.submit(() -> function.apply(connection)).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted", ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof SQLException sql) {
                throw sql;
            }
            throw new SQLException(ex.getCause());
        } catch (RejectedExecutionException ex) {
            throw new SQLException("Database is closed", ex);
        }
    }

    /** Queues a write; errors are logged. Order of writes is preserved. */
    public void run(String description, SqlAction action) {
        if (closed) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    action.run(connection);
                } catch (SQLException ex) {
                    plugin.audit().error("STORAGE", "Failed to " + description, ex);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // shutting down
        }
    }

    /** Waits for queued writes and closes the connection. */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Database queue did not finish in time");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        try {
            connection.close();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not close database: " + ex.getMessage());
        }
    }
}
