package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Locale;

/**
 * DashboardActivity provides an overview of health vitals and quick access to features.
 */
public class DashboardActivity extends BaseActivity {

    private static final String TAG = "DashboardActivity";
    private TextView tvGreeting, tvHeartRate, tvLatestBP, tvMedReminder;
    private MaterialButton btnLogVitals, btnMeds, btnAppointments, btnSOS;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration userListener, vitalsListener, medListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Security Check: Ensure user is authenticated
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found, redirecting to Login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();
        userId = user.getUid();

        initViews();
        setupNavigation();
        loadUserData();
        loadLatestData();
        
        setupBottomNavigation(R.id.nav_home);
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvHeartRate = findViewById(R.id.tvHeartRateSummary);
        tvLatestBP = findViewById(R.id.tvBPSummary);
        tvMedReminder = findViewById(R.id.tvNextMed);
        
        btnLogVitals = findViewById(R.id.btnNavVitals);
        btnMeds = findViewById(R.id.btnNavMeds);
        btnAppointments = findViewById(R.id.btnNavAppointments);
        btnSOS = findViewById(R.id.btnNavSOS);
    }

    private void setupNavigation() {
        btnLogVitals.setOnClickListener(v -> startActivity(new Intent(this, VitalsActivity.class)));
        btnMeds.setOnClickListener(v -> startActivity(new Intent(this, MedicationActivity.class)));
        btnAppointments.setOnClickListener(v -> startActivity(new Intent(this, AppointmentsActivity.class)));
        btnSOS.setOnClickListener(v -> startActivity(new Intent(this, SOSActivity.class)));
    }

    private void loadUserData() {
        if (userId == null) return;
        
        // Detach existing if any
        if (userListener != null) {
            userListener.remove();
            ListenerRegistry.INSTANCE.unregister("dashboard_user_" + userId);
        }

        // Use a SnapshotListener for real-time updates and better reliability
        userListener = db.collection("users").document(userId).addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening for user data", error);
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String name = documentSnapshot.getString("firstName");
                if (name == null) name = documentSnapshot.getString("name"); // Fallback

                if (name != null && !name.isEmpty()) {
                    tvGreeting.setText(String.format("Hello, %s", name));
                } else {
                    tvGreeting.setText("Hello, User");
                }
            }
        });
        
        ListenerRegistry.INSTANCE.register("dashboard_user_" + userId, userListener);
    }

    private void loadLatestData() {
        if (userId == null) return;

        if (vitalsListener != null) {
            vitalsListener.remove();
            ListenerRegistry.INSTANCE.unregister("dashboard_vitals_" + userId);
        }

        // Load latest Vitals with real-time updates
        vitalsListener = db.collection("users").document(userId).collection("vitals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) return;
                    
                    com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    Object hrObj = doc.get("heartRate");
                    Object bpObj = doc.get("bloodPressure");
                    
                    if (hrObj != null) tvHeartRate.setText(String.format(Locale.getDefault(), "Heart Rate: %s BPM", hrObj.toString()));
                    if (bpObj != null) tvLatestBP.setText(String.format("BP: %s", bpObj.toString()));
                });
        
        ListenerRegistry.INSTANCE.register("dashboard_vitals_" + userId, vitalsListener);

        if (medListener != null) {
            medListener.remove();
            ListenerRegistry.INSTANCE.unregister("dashboard_meds_" + userId);
        }

        // Load next Medication
        medListener = db.collection("users").document(userId).collection("medications")
                .limit(1)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) return;
                    
                    String name = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                    String time = queryDocumentSnapshots.getDocuments().get(0).getString("time");
                    tvMedReminder.setText(String.format("%s at %s", name, time));
                });
        
        ListenerRegistry.INSTANCE.register("dashboard_meds_" + userId, medListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Fix 4 cleanup placeholder
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) userListener.remove();
        if (vitalsListener != null) vitalsListener.remove();
        if (medListener != null) medListener.remove();
        
        ListenerRegistry.INSTANCE.unregister("dashboard_user_" + userId);
        ListenerRegistry.INSTANCE.unregister("dashboard_vitals_" + userId);
        ListenerRegistry.INSTANCE.unregister("dashboard_meds_" + userId);
    }
}
