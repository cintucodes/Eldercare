package com.example.eldercare;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SosOfflineWorker extends Worker {
    private static final String TAG = "SosOfflineWorker";

    public SosOfflineWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Result.failure();

        ElderCareDatabase db = ElderCareDatabase.getInstance(getApplicationContext());
        List<PendingSosAlert> pendingAlerts = db.pendingSosAlertDao().getAllPending();

        if (pendingAlerts == null || pendingAlerts.isEmpty()) {
            return Result.success();
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        for (PendingSosAlert alert : pendingAlerts) {
            Map<String, Object> sosEvent = new HashMap<>();
            sosEvent.put("timestamp", com.google.firebase.Timestamp.now());
            sosEvent.put("location", alert.latitude + "," + alert.longitude);
            sosEvent.put("locationUrl", "https://maps.google.com/?q=" + alert.latitude + "," + alert.longitude);
            sosEvent.put("status", "Sent (Offline Recovered)");

            firestore.collection("users").document(uid).collection("sos_history").add(sosEvent);

            Map<String, Object> alertMap = new HashMap<>();
            alertMap.put("title", "SOS EMERGENCY (RECOVERED)!");
            alertMap.put("message", "The elder triggered an SOS alert while offline.");
            alertMap.put("type", "health_sos");
            alertMap.put("severity", "critical");
            alertMap.put("isResolved", false);
            alertMap.put("isAcknowledged", false); // Improvement 3
            alertMap.put("timestamp", com.google.firebase.Timestamp.now());
            alertMap.put("createdBy", uid);

            firestore.collection("users").document(uid).collection("alerts").add(alertMap);
        }

        db.pendingSosAlertDao().deleteAll();
        Log.d(TAG, "Offline SOS alerts synced to Firestore.");

        return Result.success();
    }
}
