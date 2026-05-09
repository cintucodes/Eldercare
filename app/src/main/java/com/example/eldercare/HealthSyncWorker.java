package com.example.eldercare;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.health.connect.client.HealthConnectClient;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class HealthSyncWorker extends Worker {
    private static final String TAG = "HealthSyncWorker";

    public HealthSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting Health Connect sync...");
        
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return Result.failure();

        HealthConnectClient healthConnectClient = HealthConnectClient.getOrCreate(getApplicationContext());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        RuleEngine ruleEngine = new RuleEngine(userId);

        try {
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(15, ChronoUnit.MINUTES);

            // 1. Sync Heart Rate
            HealthConnectBridge.readHeartRate(healthConnectClient, startTime, endTime, 
                response -> {
                    response.getRecords().forEach(record -> {
                        if (!record.getSamples().isEmpty()) {
                            long hr = record.getSamples().get(0).getBeatsPerMinute();
                            saveVital(db, userId, AppConstants.FIELD_HEART_RATE, hr);
                            ruleEngine.checkVitals((int) hr, 0, 0);
                        }
                    });
                },
                e -> Log.e(TAG, "Error reading heart rate", e)
            );

            // 2. Sync Steps
            HealthConnectBridge.readSteps(healthConnectClient, startTime, endTime,
                totalSteps -> {
                    if (totalSteps > 0) {
                        saveVital(db, userId, AppConstants.FIELD_STEPS, totalSteps);
                        ruleEngine.checkVitals(0, totalSteps.intValue(), 0);
                    }
                },
                e -> Log.e(TAG, "Error reading steps", e)
            );

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error syncing data", e);
            return Result.retry();
        }
    }

    private void saveVital(FirebaseFirestore db, String userId, String field, Object value) {
        Map<String, Object> vital = new HashMap<>();
        vital.put(field, value);
        vital.put(AppConstants.FIELD_RECORDED_AT, com.google.firebase.Timestamp.now());

        db.collection(AppConstants.COLLECTION_USERS)
                .document(userId)
                .collection(AppConstants.COLLECTION_VITALS)
                .add(vital);
    }
}
