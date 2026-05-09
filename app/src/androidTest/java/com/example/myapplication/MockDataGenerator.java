package com.example.myapplication;

import androidx.test.core.app.ActivityScenario;

import com.example.eldercare.ElderMainActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility "test" to inject mock health data into Firestore for the current logged-in user
 * and then automatically launch the app UI to show the results.
 */
public class MockDataGenerator {

    @Test
    public void injectMockDataAndShowApp() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            throw new RuntimeException("No user logged in. Please log in on the emulator first.");
        }

        SecureRandom random = new SecureRandom();

        // 1. Inject Vitals (vitals sub-collection) - Used for History/Charts
        for (int i = 0; i < 10; i++) {
            Map<String, Object> vital = new HashMap<>();
            double sys = 115.0 + random.nextInt(20);
            double dia = 75.0 + random.nextInt(15);
            vital.put("heartRate", (double) (65 + random.nextInt(25)));
            vital.put("systolicBP", sys);
            vital.put("diastolicBP", dia);
            vital.put("bloodGlucose", 85.0 + random.nextInt(40));
            vital.put("bpStatus", "Normal");
            vital.put("recordedAt", Timestamp.now());
            // Store timestamps spanning the last 10 hours
            vital.put("timestamp", System.currentTimeMillis() - (i * 3600000));

            db.collection("users").document(uid).collection("vitals").add(vital);
        }

        // 2. Inject Health Records (healthRecords sub-collection) - Used for Dashboard and Charts
        String[] types = {"steps", "heart_rate", "glucose", "blood_pressure"};
        for (String type : types) {
            // Inject 7 days of data for each type to populate charts
            for (int day = 0; day < 7; day++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -day);
                
                Map<String, Object> record = new HashMap<>();
                record.put("type", type);
                record.put("timestamp", new Timestamp(cal.getTime()));
                
                if (type.equals("blood_pressure")) {
                    record.put("value", (115 + random.nextInt(20)) + "/" + (70 + random.nextInt(15)));
                } else if (type.equals("steps")) {
                    // Use Double to ensure compatibility with getDouble()
                    record.put("value", (double) (2000 + random.nextInt(4000)));
                } else if (type.equals("glucose")) {
                    record.put("value", 80.0 + random.nextInt(50));
                } else { // heart_rate
                    record.put("value", 60.0 + random.nextInt(40));
                }

                db.collection("users").document(uid).collection("healthRecords").add(record);
            }
        }

        // 3. Inject Mock Medications
        String[] medNames = {"Aspirin", "Metformin", "Lisinopril"};
        for (String name : medNames) {
            Map<String, Object> med = new HashMap<>();
            med.put("name", "Demo " + name);
            med.put("dosage", "100mg");
            med.put("frequency", "Daily");
            med.put("time", "09:00");
            med.put("isActive", true);
            med.put("takenToday", false);
            db.collection("users").document(uid).collection("medications").add(med);
        }

        // 4. Inject Mock Appointments
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 2);
        
        Map<String, Object> appt = new HashMap<>();
        appt.put("doctorName", "Dr. Smith");
        appt.put("specialty", "Cardiologist");
        appt.put("dateTime", new Timestamp(cal.getTime()));
        appt.put("location", "City General Hospital");
        appt.put("doctorPhone", "555-0123");
        appt.put("notes", "Regular checkup. Bring recent vitals.");
        appt.put("reminderEnabled", true);
        db.collection("users").document(uid).collection("appointments").add(appt);

        // 5. Automatically launch the Activity so you can SEE it
        ActivityScenario.launch(ElderMainActivity.class);
        
        // Keep the app open for a while to observe the data
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
