package me.uc_hussein.ultrasbroadcast.storage;

import me.uc_hussein.ultrasbroadcast.model.Announcement;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementStatus;
import me.uc_hussein.ultrasbroadcast.model.AnnouncementType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AnnouncementRepository {
    private final DatabaseManager database;

    public AnnouncementRepository(DatabaseManager database) {
        this.database = database;
    }

    public List<Announcement> loadAll() throws SQLException {
        return database.call(connection -> {
            List<Announcement> result = new ArrayList<>();
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM announcements WHERE deleted = 0 ORDER BY id")) {
                while (rs.next()) {
                    result.add(read(rs));
                }
            }
            return result;
        });
    }

    /** Highest id ever used, including deleted records. */
    public long maxId() throws SQLException {
        return database.call(connection -> {
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 0) FROM announcements")) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        });
    }

    /** Queues an insert-or-update of a snapshot. */
    public void save(Announcement snapshot) {
        database.run("save announcement #" + snapshot.id(), connection -> upsert(connection, snapshot));
    }

    private void upsert(Connection connection, Announcement a) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO announcements "
                + "(id,message,type,location,creator_uuid,creator_name,target,created_at,started_at,ended_at,end_at,"
                + "duration_ms,status,republish_count,views,deleted,deleted_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, a.id());
            ps.setString(2, a.message());
            ps.setString(3, a.type().name());
            ps.setString(4, a.location().name());
            ps.setString(5, a.creatorUuid().toString());
            ps.setString(6, a.creatorName());
            ps.setString(7, a.target());
            ps.setLong(8, a.createdAt());
            ps.setLong(9, a.startedAt());
            ps.setLong(10, a.endedAt());
            ps.setLong(11, a.endAt());
            ps.setLong(12, a.durationMs());
            ps.setString(13, a.status().name());
            ps.setInt(14, a.republishCount());
            ps.setLong(15, a.views());
            ps.setInt(16, a.deleted() ? 1 : 0);
            ps.setLong(17, a.deletedAt());
            ps.executeUpdate();
        }
    }

    /** Removes rows that were soft-deleted before the cutoff. */
    public void purgeDeletedBefore(long cutoff) {
        database.run("purge deleted announcements", connection -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM announcements WHERE deleted = 1 AND deleted_at < ?")) {
                ps.setLong(1, cutoff);
                ps.executeUpdate();
            }
        });
    }

    private Announcement read(ResultSet rs) throws SQLException {
        AnnouncementType type = parseType(rs.getString("type"));
        AnnouncementType location = parseType(rs.getString("location"));
        UUID creator;
        try {
            creator = UUID.fromString(rs.getString("creator_uuid"));
        } catch (IllegalArgumentException ex) {
            creator = Announcement.CONSOLE_UUID;
        }
        return new Announcement(rs.getLong("id"), rs.getString("message"), type, location, creator,
                rs.getString("creator_name"), rs.getString("target"), rs.getLong("created_at"),
                rs.getLong("started_at"), rs.getLong("ended_at"), rs.getLong("end_at"), rs.getLong("duration_ms"),
                AnnouncementStatus.parse(rs.getString("status")), rs.getInt("republish_count"), rs.getLong("views"),
                rs.getInt("deleted") == 1, rs.getLong("deleted_at"));
    }

    private static AnnouncementType parseType(String value) {
        try {
            return AnnouncementType.valueOf(value);
        } catch (RuntimeException ex) {
            return AnnouncementType.CHAT;
        }
    }
}
