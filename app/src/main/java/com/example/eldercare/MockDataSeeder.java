package com.example.eldercare;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MockDataSeeder {

    private static final String TAG = "MockDataSeeder";

    public static Task<Void> seedDemoData(String elderUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        Random random = new Random();
        Calendar cal = Calendar.getInstance();

        // 1. Seed Vitals (30 days)
        for (int i = 0; i < 30; i++) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            Date date = cal.getTime();
            Timestamp ts = new Timestamp(date);

            Map<String, Object> vital = new HashMap<>();
            
            // Heart Rate logic
            int hr = 62 + random.nextInt(30); // 62-91
            if (i == 7 || i == 14 || i == 21) hr = 105;
            vital.put("heartRate", hr);

            // Steps logic
            int steps = 2000 + random.nextInt(6501); // 2000-8500
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                steps = 1500 + random.nextInt(1500); // lower on weekends
            }
            vital.put("steps", steps);

            // BP (3 times per week)
            if (i % 2 == 0) {
                int sys = 110 + random.nextInt(15); // 110-125
                int dia = 70 + random.nextInt(10);  // 70-80
                if (i >= 14 && i <= 21 && random.nextBoolean()) {
                    sys = 135; // Borderline
                    dia = 85;
                }
                vital.put("systolicBP", sys);
                vital.put("diastolicBP", dia);
                vital.put("bpStatus", sys >= 140 ? "Abnormal" : (sys >= 120 ? "Borderline" : "Normal"));
            }

            // Glucose (3 times per week)
            if (i % 3 == 0) {
                double gluc = 4.5 + (random.nextDouble() * 1.5); // 4.5-6.0
                if (i == 20) gluc = 7.5; // Abnormal
                vital.put("bloodGlucose", gluc);
                vital.put("glucoseStatus", gluc >= 7.0 ? "Abnormal" : (gluc >= 5.6 ? "Borderline" : "Normal"));
            }

            vital.put("timestamp", ts.getSeconds() * 1000);
            vital.put("recordedAt", ts);

            DocumentReference ref = db.collection("users").document(elderUid).collection("vitals").document();
            batch.set(ref, vital);
        }

        // 2. Seed Medications
        seedMedication(batch, db, elderUid, "Amlodipine 5mg", "08:00", "daily");
        seedMedication(batch, db, elderUid, "Metformin 500mg", "07:00, 19:00", "twice daily");
        seedMedication(batch, db, elderUid, "Aspirin 75mg", "12:00", "daily");

        // 3. Seed SOS Alerts
        seedAlert(batch, db, elderUid, 15, "resolved");
        seedAlert(batch, db, elderUid, 3, "acknowledged");

        return batch.commit();
    }

    private static void seedMedication(WriteBatch batch, FirebaseFirestore db, String uid, String name, String time, String freq) {
        Map<String, Object> med = new HashMap<>();
        med.put("name", name);
        med.put("time", time);
        med.put("frequency", freq);
        med.put("isSeeded", true);
        DocumentReference ref = db.collection("users").document(uid).collection("medications").document();
        batch.set(ref, med);
    }

    private static void seedAlert(WriteBatch batch, FirebaseFirestore db, String uid, int daysAgo, String status) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        
        Map<String, Object> alert = new HashMap<>();
        alert.put("status", status);
        alert.put("timestamp", new Timestamp(cal.getTime()));
        alert.put("latitude", -1.5177 + (new Random().nextDouble() * 0.01));
        alert.put("longitude", 37.2634 + (new Random().nextDouble() * 0.01));
        alert.put("type", "SOS");
        alert.put("isSeeded", true);

        DocumentReference ref = db.collection("users").document(uid).collection("alerts").document();
        batch.set(ref, alert);
    }

    public static Task<Void> clearDemoData(String elderUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Task<Void>> tasks = new ArrayList<>();

        tasks.add(deleteCollection(db, elderUid, "vitals"));
        tasks.add(deleteCollection(db, elderUid, "medications"));
        tasks.add(deleteCollection(db, elderUid, "alerts"));

        return Tasks.whenAll(tasks);
    }

    private static Task<Void> deleteCollection(FirebaseFirestore db, String uid, String collection) {
        return db.collection("users").document(uid).collection(collection)
                .get()
                .continueWithTask(task -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult()) {
                        batch.delete(doc.getReference());
                    }
                    return batch.commit();
                });
    }
}
