package com.example.eldercare;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return Result.failure();

        ElderCareDatabase db = ElderCareDatabase.getInstance(getApplicationContext());
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Sync Vitals - UPDATED TO MATCH NEW SCHEMA
        List<VitalEntity> unsyncedVitals = db.vitalDao().getUnsynced();
        for (VitalEntity vital : unsyncedVitals) {
            // We need to save each vital type as a separate record for the dashboard to work
            syncIndividualVital(firestore, userId, "heart_rate", (double) vital.heartRate, vital.timestamp);
            syncIndividualVital(firestore, userId, "blood_pressure", vital.bloodPressure, vital.timestamp);
            syncIndividualVital(firestore, userId, "glucose", vital.bloodGlucose, vital.timestamp);
            
            vital.syncedToFirestore = true;
            db.vitalDao().update(vital);
        }

        // Sync Medications
        List<MedicationEntity> unsyncedMeds = db.medicationDao().getUnsynced();
        for (MedicationEntity med : unsyncedMeds) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", med.name);
            data.put("dosage", med.dosage);
            data.put("time", med.time);
            data.put("isActive", true);
            data.put("timestamp", Timestamp.now());

            try {
                Tasks.await(firestore.collection("users").document(userId).collection("medications")
                        .add(data));
                med.syncedToFirestore = true;
                db.medicationDao().update(med);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to sync medication", e);
            }
        }

        return Result.success();
    }

    private void syncIndividualVital(FirebaseFirestore firestore, String userId, String type, Object value, long timestamp) {
        Map<String, Object> record = new HashMap<>();
        record.put("type", type);
        record.put("value", value);
        record.put("timestamp", new Timestamp(new java.util.Date(timestamp)));
        
        try {
            Tasks.await(firestore.collection("users").document(userId).collection("vitals").add(record));
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync " + type, e);
        }
    }
}
