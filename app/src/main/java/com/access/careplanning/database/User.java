package com.access.careplanning.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a signed-in user
 */
@Entity
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Unique ID from the Google account
     */
    @ColumnInfo(name = "google_id")
    private String googleId;

    private String name;

    public User(String googleId, String name) {
        this.googleId = googleId;
        this.name = name;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the unique ID from the Google account, else null if not set
     */
    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
