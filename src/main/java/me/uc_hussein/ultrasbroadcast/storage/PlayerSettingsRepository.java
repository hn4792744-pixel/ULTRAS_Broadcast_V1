package me.uc_hussein.ultrasbroadcast.storage;

import me.uc_hussein.ultrasbroadcast.model.PlayerSettings;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class PlayerSettingsRepository {
    private final DatabaseManager database;

    public PlayerSettingsRepository(DatabaseManager database) {
        this.database = database;
    }

    /** Blocking; call from an async thread. @return null when the player has no saved row */
    public PlayerSettings load(UUID uuid) throws SQLException {
        return database.call(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_settings WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return new PlayerSettings(uuid, rs.getString("name"), rs.getString("language"),
                            rs.getInt("sounds") == 1, rs.getInt("chat") == 1, rs.getInt("screen") == 1,
                            rs.getInt("hearts") == 1, rs.getLong("updated_at"));
                }
            }
        });
    }

    /** Queues a save of a snapshot. */
    public void save(PlayerSettings snapshot) {
        database.run("save settings of " + snapshot.name(), connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO player_settings "
                    + "(uuid,name,language,sounds,chat,screen,hearts,updated_at) VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setString(1, snapshot.uuid().toString());
                ps.setString(2, snapshot.name());
                ps.setString(3, snapshot.language());
                ps.setInt(4, snapshot.sounds() ? 1 : 0);
                ps.setInt(5, snapshot.chat() ? 1 : 0);
                ps.setInt(6, snapshot.screen() ? 1 : 0);
                ps.setInt(7, snapshot.hearts() ? 1 : 0);
                ps.setLong(8, snapshot.updatedAt());
                ps.executeUpdate();
            }
        });
    }
}
