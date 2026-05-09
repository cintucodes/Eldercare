package com.example.eldercare;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VitalsTrendAnalyzer {

    public static void analyzeTrend(String userId, List<VitalEntity> recentVitals) {
        if (recentVitals == null || recentVitals.size() < 3) return;

        checkBPTrend(userId, recentVitals);
        checkGlucoseTrend(userId, recentVitals);
    }

    private static void checkBPTrend(String userId, List<VitalEntity> vitals) {
        boolean increasing = true;
        int lastSystolic = -1;

        for (int i = vitals.size() - 1; i >= 0; i--) {
            String[] parts = vitals.get(i).bloodPressure.split("/");
            int currentSys = Integer.parseInt(parts[0]);
            
            if (lastSystolic != -1 && currentSys <= lastSystolic) {
                increasing = false;
                break;
            }
            lastSystolic = currentSys;
        }

        if (increasing) {
            sendTrendAlert(userId, "Health Trend Warning", 
                "Blood pressure has been steadily increasing over the last " + vitals.size() + " readings.");
        }
    }

    private static void checkGlucoseTrend(String userId, List<VitalEntity> vitals) {
        int highCount = 0;
        for (VitalEntity v : vitals) {
            if (v.bloodGlucose >= 7.0) highCount++;
        }

        if (highCount >= 3) {
            sendTrendAlert(userId, "Health Pattern Detected", 
                "Consistent high glucose levels detected (" + highCount + " of last " + vitals.size() + " readings).");
        }
    }

    private static void sendTrendAlert(String userId, String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", title);
        alert.put("message", message);
        alert.put("type", "trend_warning");
        alert.put("severity", "medium");
        alert.put("isResolved", false);
        alert.put("isAcknowledged", false); // Improvement 3: Alert acknowledgement
        alert.put("timestamp", Timestamp.now());
        alert.put("createdBy", userId);

        db.collection("users").document(userId).collection("alerts").add(alert);
    }
}
