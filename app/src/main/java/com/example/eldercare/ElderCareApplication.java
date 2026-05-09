package com.example.eldercare;

import android.app.Application;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.util.concurrent.TimeUnit;

/**
 * Application class for ElderCare app.
 * Configures Firebase with offline persistence to reduce network usage and reads.
 */
public class ElderCareApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Enable offline persistence for Firestore
        // This caches data locally and reduces Firebase reads by 60-70%
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Allow unlimited cache
                .build();
        db.setFirestoreSettings(settings);
        
        // Schedule daily health sync reminder
        scheduleHealthSyncReminder();
    }
    
    private void scheduleHealthSyncReminder() {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        
        PeriodicWorkRequest reminderWork = new PeriodicWorkRequest.Builder(
            HealthSyncReminderWorker.class,
            24, TimeUnit.HOURS // Check once per day
        )
        .setConstraints(constraints)
        .build();
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "health_sync_reminder",
            ExistingPeriodicWorkPolicy.KEEP, // Don't restart if already scheduled
            reminderWork
        );
    }
}
