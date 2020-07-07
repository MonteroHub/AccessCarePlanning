package com.access.careplanning.database;

import android.app.Application;
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {User.class, TimeRange.class, Battery.class}, version = 1, exportSchema = false)
public abstract class CarePlanningRoomDatabase extends RoomDatabase {

    public abstract CarePlanningDao taskDao();

    private static volatile CarePlanningRoomDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;

    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static CarePlanningRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (CarePlanningRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            CarePlanningRoomDatabase.class, "task_database")
                            //add optional init callback  .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /*
     *If we need to prepopulate the database with some init data, do here in this callback
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            databaseWriteExecutor.execute(() -> {
                // PrePopulate the database in the background.
            });
        }
    };*/
}
