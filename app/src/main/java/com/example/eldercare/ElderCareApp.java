package com.example.eldercare;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ElderCareApp extends Application implements Configuration.Provider {
    private static final String TAG = "ElderCareApp";
    private final ExecutorService diskIoExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Bug 6 Fix: Move non-critical initialization off the main thread
        diskIoExecutor.execute(() -> {
            setupCrashHandler();
            
            // Initialize UserSessionManager
            UserSessionManager.getInstance().init(this);
            Log.d(TAG, "UserSessionManager initialized in background");

            // Schedule background synchronization
            scheduleSync();
            
            // Create notification channels early
            NotificationChannelHelper.createNotificationChannels(this);
        });
    }

    private void setupCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String crashLog = sw.toString();

            try {
                File file = new File(getExternalFilesDir(null), "crash_log.txt");
                FileWriter fw = new FileWriter(file, false);
                fw.write("CRASH TIME: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(new Date()) + "\n\n");
                fw.write(crashLog);
                fw.close();
                Log.e("CRASH", "Crash saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                Log.e("CRASH", "Failed to save crash log", e);
            }

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }

    private void scheduleSync() {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                    SyncWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "ElderCareSync",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
            );
            Log.d(TAG, "Background sync scheduled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule sync", e);
        }
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        // Bug 6 Fix: Use a background executor for WorkManager as well
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setExecutor(diskIoExecutor)
                .build();
    }
}
