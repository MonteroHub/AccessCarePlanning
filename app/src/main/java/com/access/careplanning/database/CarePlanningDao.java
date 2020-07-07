package com.access.careplanning.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

/**
 * Data Access Object for the app queries and actions
 */
@Dao
public interface CarePlanningDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(User user);

    @Query("SELECT * from user WHERE google_id= :googleId")
    User getUser(String googleId);

    @Query("SELECT * from user WHERE google_id= :googleId")
    LiveData<User> getUserLiveData(String googleId);


    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(TimeRange timeRange);

    @Update
    void update(TimeRange timeRange);

    @Query("SELECT * from time_range WHERE user_id == :userId")
    TimeRange getTimeRange(int userId);

    @Query("SELECT * from time_range WHERE user_id == :userId")
    LiveData<TimeRange> getTimeRangeLiveData(int userId);


    @Query("SELECT * from battery WHERE user_id == :userId")
    LiveData<Battery> getBatteryLiveData(int userId);

    @Query("SELECT * from battery WHERE user_id == :userId")
    Battery getBattery(int userId);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(Battery battery);

    @Update
    void update(Battery battery);

}
