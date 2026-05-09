package com.example.eldercare;

import com.example.eldercare.FirestoreHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AlertManager {

    private static AlertManager instance;
    private final FirebaseFirestore db;
    private final com.example.eldercare.FirestoreHelper firestoreHelper;

    private AlertManager() {
        db = FirebaseFirestore.getInstance();
        firestoreHelper = new FirestoreHelper();
    }

    public static synchronized AlertManager getInstance() {
        if (instance == null) {
            instance = new AlertManager();
        }
        return instance;
    }

    public void createVitalAlert(String userId, String vitalType, String value, boolean isAbnormal) {
        String title = isAbnormal ? "Critical Vital Reading" : "Vital Warning";
        String message = "Your " + vitalType + " reading of " + value + " is " + (isAbnormal ? "critical." : "borderline.");
        String type = isAbnormal ? "CRITICAL" : "WARNING";
        saveAlert(userId, title, message, type);
    }

    public void createMedicationMissedAlert(String userId, String medicationName) {
        saveAlert(userId, "Missed Medication", "You missed your dose of " + medicationName + ".", "WARNING");
    }

    public void createAppointmentAlert(String userId, String doctorName, int hoursUntil) {
        saveAlert(userId, "Appointment Reminder", "You have an appointment with " + doctorName + " in " + hoursUntil + " hours.", "INFO");
    }

    public void createSOSAlert(String userId, String userName) {
        saveAlert(userId, "SOS ALERT", "Emergency SOS triggered by " + userName, "CRITICAL");
    }

    private void saveAlert(String userId, String title, String message, String type) {
        if (userId == null) return;

        Map<String, Object> alert = new HashMap<>();
        alert.put("title", title);
        alert.put("message", message);
        alert.put("type", type);
        alert.put("timestamp", Timestamp.now());
        alert.put("dismissed", false);
        alert.put("isResolved", false); // Consistency fix
        alert.put("isAcknowledged", false); // Improvement 3

        db.collection("users").document(userId).collection("alerts").add(alert);
    }
}
