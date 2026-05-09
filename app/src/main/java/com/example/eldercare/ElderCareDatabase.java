package com.example.eldercare;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {VitalEntity.class, MedicationEntity.class, HealthSnapshot.class, PendingSosAlert.class}, version = 3, exportSchema = false)
public abstract class ElderCareDatabase extends RoomDatabase {
    private static ElderCareDatabase instance;

    public abstract VitalDao vitalDao();
    public abstract MedicationDao medicationDao();
    public abstract HealthSnapshotDao healthSnapshotDao();
    public abstract PendingSosAlertDao pendingSosAlertDao();

    public static synchronized ElderCareDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    ElderCareDatabase.class, "eldercare_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
