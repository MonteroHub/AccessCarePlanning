package com.access.careplanning.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Represents a time range with start and end (hours and minutes), for a given user
 */
@Entity(tableName = "time_range", foreignKeys = @ForeignKey(entity = User.class,
        parentColumns = "id",
        childColumns = "user_id",
        onDelete = ForeignKey.NO_ACTION))
public class TimeRange {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "start_hour")
    private int startHour;

    @ColumnInfo(name = "start_minute")
    private int startMinute;

    @ColumnInfo(name = "end_hour")
    private int endHour;

    @ColumnInfo(name = "end_minute")
    private int endMinute;

    public TimeRange() {
    }

    public TimeRange(int hour, int min, int hourEnd, int minuteEnd) {
        startHour = hour;
        startMinute = min;
        endHour = hourEnd;
        endMinute = minuteEnd;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }
}
