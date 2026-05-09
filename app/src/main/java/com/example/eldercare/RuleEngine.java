package com.example.eldercare;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RuleEngine {
    private static final String TAG = "RuleEngine";
    private final FirebaseFirestore db;
    private final String userId;

    public RuleEngine(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    public void checkVitals(int heartRate, int steps, double sleepHours) {
        // Heart Rate Rules
        if (heartRate > 0 && (heartRate < 60 || heartRate > 100)) {
            createAlert("Heart Rate Alert", "Heart rate is " + heartRate + " BPM.", "RED", heartRate);
        }

        // Steps Rules (Check if it's after 6 PM and steps are low)
        // TEMPORARILY DISABLED FOR DEMO - Prevents false positives on new accounts
        // Calendar now = Calendar.getInstance();
        // if (now.get(Calendar.HOUR_OF_DAY) >= 18 && steps > 0 && steps < 500) {
        //     createAlert("Inactivity Alert", "Only " + steps + " steps recorded by 6 PM.", "AMBER", steps);
        // }

        // Sleep Rules
        if (sleepHours > 0 && sleepHours < 5.0) {
            createAlert("Sleep Alert", "Less than 5 hours of sleep recorded (" + sleepHours + "h).", "AMBER", sleepHours);
        }
    }

    private void createAlert(String type, String message, String severity, Object value) {
        if (userId == null) return;

        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type);
        alert.put("message", message);
        alert.put("severity", severity);
        alert.put("value", value);
        alert.put("timestamp", Timestamp.now());
        alert.put("acknowledged", false);

        db.collection("users")
                .document(userId)
                .collection("alerts")
                .add(alert)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Alert created: " + type))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating alert", e));
    }
}
