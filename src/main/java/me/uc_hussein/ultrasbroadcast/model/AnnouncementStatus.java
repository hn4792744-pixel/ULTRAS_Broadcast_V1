package me.uc_hussein.ultrasbroadcast.model;

public enum AnnouncementStatus {
    ACTIVE, FINISHED, CANCELLED;

    public static AnnouncementStatus parse(String value) {
        try {
            return valueOf(value);
        } catch (RuntimeException ex) {
            return FINISHED;
        }
    }
}
