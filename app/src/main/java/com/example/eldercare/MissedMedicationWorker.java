package com.example.eldercare;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class MissedMedicationWorker extends Worker {
    private static final String TAG = "MissedMedicationWorker";

    public MissedMedicationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String medId = getInputData().getString("medId");
        String userId = getInputData().getString("userId");
        String medName = getInputData().getString("medName");

        if (medId == null || userId == null) return Result.failure();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        try {
            db.collection("users").document(userId).collection("medications").document(medId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if (!"taken".equals(status)) {
                            db.collection("users").document(userId).collection("medications").document(medId)
                                    .update("status", "missed");

                            Map<String, Object> alert = new HashMap<>();
                            alert.put("title", "Missed Medication");
                            alert.put("message", "Medication " + medName + " was missed.");
                            alert.put("type", "medication_missed");
                            alert.put("severity", "high");
                            alert.put("isResolved", false);
                            alert.put("isAcknowledged", false); // Improvement 3
                            alert.put("timestamp", Timestamp.now());

                            db.collection("users").document(userId).collection("alerts").add(alert);
                            Log.d(TAG, "Medication marked as missed and caregiver notified.");
                        }
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "Failed to check medication status", e));

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in MissedMedicationWorker", e);
            return Result.retry();
        }
    }
}
