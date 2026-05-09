package com.example.eldercare;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

/**
 * Generates realistic test data to simulate Health Connect integration
 * without requiring a physical wearable device
 */
public class HealthConnectTestDataGenerator {
    
    private final Context context;
    private final FirebaseFirestore db;
    private final String userId;
    
    public HealthConnectTestDataGenerator(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.userId = FirebaseAuth.getInstance().getUid();
    }
    
    /**
     * Simulates a full day of Health Connect data with realistic patterns
     */
    public void generateRealisticDayData(OnCompleteListener listener) {
        if (userId == null) {
            listener.onError("Not logged in");
            return;
        }
        
        List<Map<String, Object>> vitals = new ArrayList<>();
        Random random = new Random();
        long now = System.currentTimeMillis();
        
        // Morning vitals (6 AM - 9 AM)
        vitals.add(createVital("heartRate", 65 + random.nextInt(10), now - 12 * 3600000));
        vitals.add(createVital("steps", 500 + random.nextInt(500), now - 12 * 3600000));
        vitals.add(createVital("bloodGlucose", 85 + random.nextInt(15), now - 12 * 3600000));
        
        // Midday vitals (12 PM - 2 PM)
        vitals.add(createVital("heartRate", 70 + random.nextInt(15), now - 6 * 3600000));
        vitals.add(createVital("steps", 3000 + random.nextInt(2000), now - 6 * 3600000));
        
        // Evening vitals (6 PM - 8 PM)
        vitals.add(createVital("heartRate", 68 + random.nextInt(12), now - 2 * 3600000));
        vitals.add(createVital("steps", 6000 + random.nextInt(3000), now - 2 * 3600000));
        vitals.add(createVital("bloodGlucose", 95 + random.nextInt(20), now - 2 * 3600000));
        
        // Sleep data (last night)
        vitals.add(createVital("sleepHours", 6.5 + random.nextDouble() * 2, now - 8 * 3600000));
        
        // Blood pressure (morning)
        Map<String, Object> bp = new HashMap<>();
        bp.put("type", "bloodPressure");
        bp.put("systolic", 115 + random.nextInt(20));
        bp.put("diastolic", 70 + random.nextInt(15));
        bp.put("timestamp", new Date(now - 12 * 3600000));
        bp.put("source", "Health Connect");
        vitals.add(bp);
        
        // Save all vitals
        saveVitalsToFirebase(vitals, listener);
    }
    
    /**
     * Generates abnormal vitals to test alert system
     */
    public void generateAbnormalData(OnCompleteListener listener) {
        if (userId == null) {
            listener.onError("Not logged in");
            return;
        }
        
        List<Map<String, Object>> vitals = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        // High heart rate (triggers RED alert)
        vitals.add(createVital("heartRate", 105, now - 1000));
        
        // Low sleep (triggers AMBER alert)
        vitals.add(createVital("sleepHours", 4.0, now - 2000));
        
        // High glucose
        vitals.add(createVital("bloodGlucose", 180, now - 3000));
        
        // Steps
        vitals.add(createVital("steps", 2000, now - 4000));
        
        saveVitalsToFirebase(vitals, listener);
    }
    
    /**
     * Generates normal healthy vitals
     */
    public void generateNormalData(OnCompleteListener listener) {
        if (userId == null) {
            listener.onError("Not logged in");
            return;
        }
        
        List<Map<String, Object>> vitals = new ArrayList<>();
        Random random = new Random();
        long now = System.currentTimeMillis();
        
        vitals.add(createVital("heartRate", 70 + random.nextInt(10), now - 1000));
        vitals.add(createVital("steps", 7000 + random.nextInt(3000), now - 2000));
        vitals.add(createVital("bloodGlucose", 90 + random.nextInt(20), now - 3000));
        vitals.add(createVital("sleepHours", 7.0 + random.nextDouble(), now - 4000));
        
        Map<String, Object> bp = new HashMap<>();
        bp.put("type", "bloodPressure");
        bp.put("systolic", 120);
        bp.put("diastolic", 80);
        bp.put("timestamp", new Date(now - 5000));
        bp.put("source", "Health Connect");
        vitals.add(bp);
        
        saveVitalsToFirebase(vitals, listener);
    }
    
    private Map<String, Object> createVital(String type, double value, long timestamp) {
        Map<String, Object> vital = new HashMap<>();
        vital.put("type", type);
        vital.put("value", value);
        vital.put("timestamp", new Date(timestamp));
        vital.put("source", "Health Connect");
        return vital;
    }
    
    private void saveVitalsToFirebase(List<Map<String, Object>> vitals, OnCompleteListener listener) {
        int[] savedCount = {0};
        int totalVitals = vitals.size();
        
        for (Map<String, Object> vital : vitals) {
            db.collection("users").document(userId)
                .collection("vitals")
                .add(vital)
                .addOnSuccessListener(ref -> {
                    savedCount[0]++;
                    if (savedCount[0] == totalVitals) {
                        updateLastSyncTime();
                        updateLatestVitalsEntry(vitals);
                        listener.onSuccess(totalVitals);
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }
    
    private void updateLastSyncTime() {
        db.collection("users").document(userId)
            .update("lastHealthConnectSync", System.currentTimeMillis());
    }
    
    private void updateLatestVitalsEntry(List<Map<String, Object>> vitals) {
        Map<String, Object> latest = new HashMap<>();
        latest.put("lastUpdated", new Date());
        
        for (Map<String, Object> vital : vitals) {
            String type = (String) vital.get("type");
            if ("bloodPressure".equals(type)) {
                latest.put("systolicBP", vital.get("systolic"));
                latest.put("diastolicBP", vital.get("diastolic"));
            } else {
                latest.put(type, vital.get("value"));
            }
        }
        
        db.collection("users").document(userId)
            .collection("vitals")
            .document("latest_vitals_entry")
            .set(latest);
    }
    
    public interface OnCompleteListener {
        void onSuccess(int vitalsSaved);
        void onError(String error);
    }
}
