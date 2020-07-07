package com.access.careplanning.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

public class CarePlanningRepository {

    private final CarePlanningDao mCarePlanningDao;

    // Note: to unit test the Repository, you have to remove the Application
    // dependency. Recommended for commercial app, this adds complexity.
    // https://github.com/googlesamples
    public CarePlanningRepository(Application application) {
        CarePlanningRoomDatabase db = CarePlanningRoomDatabase.getDatabase(application);
        mCarePlanningDao = db.taskDao();
    }

    public void insert(User user) {
        mCarePlanningDao.insert(user);
    }

    public User getUser(String googleId) {
        return mCarePlanningDao.getUser(googleId);
    }

    public LiveData<User> getUserLiveData(String googleId) {
        return mCarePlanningDao.getUserLiveData(googleId);
    }


    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    public LiveData<TimeRange> getTimeRangeLiveData(int userId) {
        return mCarePlanningDao.getTimeRangeLiveData(userId);
    }

    public TimeRange getTimeRange(int userId) {
        return mCarePlanningDao.getTimeRange(userId);
    }


    // You must call this on a non-UI thread or your app will throw an exception.
    public void insert(TimeRange timeRange) {
        CarePlanningRoomDatabase.databaseWriteExecutor.execute(() -> {
            mCarePlanningDao.insert(timeRange);
        });
    }

    // You must call this on a non-UI thread or your app will throw an exception.
    public void update(TimeRange timeRange) {
        CarePlanningRoomDatabase.databaseWriteExecutor.execute(() -> {
            mCarePlanningDao.update(timeRange);
        });
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    public LiveData<Battery> getBatteryLiveData(int userId) {
        return mCarePlanningDao.getBatteryLiveData(userId);
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    public Battery getBattery(int userId) {
        return mCarePlanningDao.getBattery(userId);
    }


    // You must call this on a non-UI thread or your app will throw an exception.
    public void insert(Battery battery) {
        CarePlanningRoomDatabase.databaseWriteExecutor.execute(() -> {
            mCarePlanningDao.insert(battery);
        });
    }

    // You must call this on a non-UI thread or your app will throw an exception.
    public void update(Battery battery) {
        CarePlanningRoomDatabase.databaseWriteExecutor.execute(() -> {
            mCarePlanningDao.update(battery);
        });
    }

}
