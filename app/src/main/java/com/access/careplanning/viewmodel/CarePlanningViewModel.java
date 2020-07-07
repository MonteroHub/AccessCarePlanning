package com.access.careplanning.viewmodel;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.access.careplanning.database.Battery;
import com.access.careplanning.database.CarePlanningRepository;
import com.access.careplanning.database.TimeRange;
import com.access.careplanning.database.User;

import java.security.InvalidParameterException;

// ViewModels don't survive the app's process being killed in the background when the OS needs more resources.
// For UI data that needs to survive process death due to running out of resources, you can use the Saved State module for ViewModels
public class CarePlanningViewModel extends AndroidViewModel {

    private static final String TAG = "CarePlanningVM";

    /**
     * Default time range to use before a user has chosen their own range
     */
    public static final TimeRange DEFAULT_TIME_RANGE = new TimeRange(10, 0, 15, 0);

    /**
     * Default battery saving threshold
     */
    public static final int DEFAULT_BATTERY_THRESHOLD = 10;

    private static final String VALID_WIFI_QR_CODE = "WIFI_ON";


    private final CarePlanningRepository mRepository;

    private int mUserId;

    private LiveData<TimeRange> mTimeRange;
    private LiveData<Battery> mBattery;

    private int mStartHour;
    private int mStartMin;
    private int mEndHour;
    private int mEndMin;
    private int mBatteryThreshold = 0;

    private boolean mBatterySaverOn = false;

    /**
     * Interface for database initialisation completed
     */
    public interface IDatabaseInit {
        void onComplete();
    }

    public CarePlanningViewModel(Application application) {
        super(application);
        mRepository = new CarePlanningRepository(application);
    }

    /**
     * Initial the user info in the database.
     * - Create the user if this user is a new sign-in or get the user id
     * - Insert default time range data if not there for this user
     *
     * @param user
     * @param defaultTimeRange
     * @param databaseInit
     */
    public void initUser(User user, TimeRange defaultTimeRange, IDatabaseInit databaseInit) {
        new Thread(() -> {
            User existingUser = mRepository.getUser(user.getGoogleId());
            // Get the user id from an existing record, or insert a new user record
            mUserId = (existingUser != null) ? existingUser.getId() : insert(user);

            // Set up default time range for this user, insert won't overwrite if the record exists
            defaultTimeRange.setUserId(mUserId);
            insert(defaultTimeRange);

            // Mark initialisation complete
            if (databaseInit != null) {
                databaseInit.onComplete();
            }
            Log.d(TAG, "initUser complete");

        }).start();
    }

    /**
     * Insert new user
     *
     * @param user
     * @return user db id
     */
    private int insert(User user) {
        mRepository.insert(user);
        return mRepository.getUser(user.getGoogleId()).getId();
    }

    /**
     * Get the time range object. Run on background thread.
     *
     * @return time range
     */
    private TimeRange getTimeRange() {
        return mRepository.getTimeRange(mUserId);
    }

    /**
     * Get live data wrapped time range, lazy initialise.
     *
     * @return live data time range
     */
    public LiveData<TimeRange> getTimeRangeLiveData() {
        if (mTimeRange == null) {
            mTimeRange = mRepository.getTimeRangeLiveData(mUserId);
        }
        return mTimeRange;
    }

    private void insert(TimeRange timeRange) {
        mRepository.insert(timeRange);
    }

    private void update(TimeRange timeRange) {
        mRepository.update(timeRange);
    }


    public int getStartHour() {
        return mStartHour;
    }

    public int getStartMin() {
        return mStartMin;
    }

    public int getEndHour() {
        return mEndHour;
    }

    public int getEndMin() {
        return mEndMin;
    }

    /**
     * Update the start time on background thread.
     * For a more complex production app consider a long running thread or queue
     *
     * @param hour   start hour
     * @param minute start minute
     */
    public void updateStartTime(final int hour, final int minute) {
        new Thread(() -> {
            TimeRange range = getTimeRange();
            if (range != null) {
                range.setStartHour(hour);
                range.setStartMinute(minute);
                update(range);
            }
        }).start();
    }

    /**
     * Update the end time on background thread.
     * For a more complex production app consider a long running thread or queue
     *
     * @param hour   end hour
     * @param minute end minute
     */
    public void updateEndTime(final int hour, final int minute) {
        new Thread(() -> {
            TimeRange range = getTimeRange();
            if (range != null) {
                range.setEndHour(hour);
                range.setEndMinute(minute);
                update(range);
            }
        }).start();
    }


    public boolean isBatterySaverOn() {
        return mBatterySaverOn;
    }

    public void setBatterySaverOn(boolean mBatterySaverOn) {
        this.mBatterySaverOn = mBatterySaverOn;
    }

    /**
     * Get live data wrapped battery, lazy initialise to avoid recreating
     *
     * @return live data wrapped battery
     */
    public LiveData<Battery> getBattery() {
        if (mUserId > 0 && mBattery == null) {
            mBattery = mRepository.getBatteryLiveData(mUserId);
        }
        return mBattery;
    }

    private void insert(Battery battery) {
        mRepository.insert(battery);
    }

    private void update(Battery battery) {
        mRepository.update(battery);
    }

    /**
     * Update or insert the battery threshold
     *
     * @param percent
     */
    public void setBatterySaverPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new InvalidParameterException("Invalid battery percent");
        }

        new Thread(() -> {
            Battery battery = mRepository.getBattery(mUserId);
            if (battery != null) {
                battery.setThreshold(percent);
                update(battery);
            } else {
                battery = new Battery();
                battery.setUserId(mUserId);
                battery.setThreshold(percent);
                insert(battery);
            }
        }).start();
    }

    public void setStartTime(int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new InvalidParameterException("Hour or minute invalid");
        }
        if (timeToMins(hour, minute) >= timeToMins(mEndHour, mEndMin)) {
            throw new InvalidParameterException("Start time must be before the end time");
        }

        mStartHour = hour;
        mStartMin = minute;
    }

    public void setEndTime(int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new InvalidParameterException("Hour or minute invalid");
        }
        if (timeToMins(hour, minute) <= timeToMins(mStartHour, mStartMin)) {
            throw new InvalidParameterException("Start time must be before the end time");
        }

        mEndHour = hour;
        mEndMin = minute;
    }

    private int getStartTimeInMins() {
        return timeToMins(mStartHour, mStartMin);
    }

    private int getEndTimeInMins() {
        return timeToMins(mEndHour, mEndMin);
    }

    private boolean isTimeWithinPeriod(int hour, int minute) {
        final int mins = timeToMins(hour, minute);
        return mins >= getStartTimeInMins() && mins <= getEndTimeInMins();
    }

    public int getCurrentRingerMode(int hour, int minute) {
        return isTimeWithinPeriod(hour, minute)
                ? AudioManager.RINGER_MODE_VIBRATE
                : AudioManager.RINGER_MODE_NORMAL;
    }

    public static int timeToMins(int hour, int min) {
        return (hour * 60) + min;
    }

    public static boolean isValidQR(String code) {
        return code != null && code.toUpperCase().contains(VALID_WIFI_QR_CODE);
    }

}
