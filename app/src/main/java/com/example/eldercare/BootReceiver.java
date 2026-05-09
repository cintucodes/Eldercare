package com.example.eldercare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device rebooted, re-registering alarms...");
            reRegisterAlarms(context);
        }
    }

    private void reRegisterAlarms(Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).collection("medications")
                .whereEqualTo("isActive", true)
                .whereEqualTo("remindersEnabled", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String medId = doc.getId();
                        String name = doc.getString("name");
                        String dosage = doc.getString("dosage");
                        List<String> times = (List<String>) doc.get("times");

                        if (times != null) {
                            MedicationReminderScheduler.scheduleAlarms(context, medId, name, dosage, times);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching medications on boot", e));
    }
}
