package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eldercare.databinding.FragmentCaregiverOverviewBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CaregiverOverviewFragment extends Fragment {

    private static final String TAG = "CaregiverOverview";
    private FragmentCaregiverOverviewBinding binding;
    private FirebaseFirestore db;
    private String elderUid;
    private ListenerRegistration alertsListener;
    private ListenerRegistration medsListener;
    private ListenerRegistration vitalsListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    
    private ActivityFeedAdapter activityAdapter;
    private MedicationAdapter medsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCaregiverOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        
        UserSessionManager session = UserSessionManager.getInstance();
        session.init(requireContext());
        
        if (!session.isCaregiver()) return;

        elderUid = session.getLinkedElderId();

        if (elderUid == null) {
            showNotLinkedState();
            return;
        }

        hideNotLinkedState();
        setInitialLabels();
        setupRecyclerViews();
        setupSwipeRefresh();
        observeLiveStatus();
        observeMedicationAdherence();
        observeVitals();
        refreshAllData();

        binding.btnViewAlerts.setOnClickListener(v -> {
            if (getActivity() instanceof CaregiverMainActivity) {
                View navAlerts = ((CaregiverMainActivity) getActivity()).findViewById(R.id.nav_alerts);
                if (navAlerts != null) navAlerts.performClick();
            }
        });
    }

    private void showNotLinkedState() {
        if (binding == null) return;
        binding.mainContentScroll.setVisibility(View.GONE);
        binding.layoutNotLinked.setVisibility(View.VISIBLE);
        binding.btnLinkNow.setOnClickListener(v -> startActivity(new Intent(getContext(), CaregiverLinkActivity.class)));
    }

    private void hideNotLinkedState() {
        if (binding == null) return;
        binding.mainContentScroll.setVisibility(View.VISIBLE);
        binding.layoutNotLinked.setVisibility(View.GONE);
    }

    private void setInitialLabels() {
        binding.layoutHeartRate.tvVitalLabel.setText("Heart Rate");
        binding.layoutHeartRate.tvVitalUnit.setText("bpm");
        binding.layoutBP.tvVitalLabel.setText("Blood Pressure");
        binding.layoutBP.tvVitalUnit.setText("mmHg");
        binding.layoutGlucose.tvVitalLabel.setText("Glucose");
        binding.layoutGlucose.tvVitalUnit.setText("mmol/L");
        binding.layoutSteps.tvVitalLabel.setText("Steps");
        binding.layoutSteps.tvVitalUnit.setText("steps");
        binding.layoutSleep.tvVitalLabel.setText("Sleep");
        binding.layoutSleep.tvVitalUnit.setText("hrs");
    }

    private void setupRecyclerViews() {
        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        activityAdapter = new ActivityFeedAdapter(new ArrayList<>());
        binding.rvRecentActivity.setAdapter(activityAdapter);

        binding.rvMedications.setLayoutManager(new LinearLayoutManager(getContext()));
        medsAdapter = new MedicationAdapter(new ArrayList<>());
        binding.rvMedications.setAdapter(medsAdapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshAllData();
            new Handler().postDelayed(() -> { if (binding != null) binding.swipeRefresh.setRefreshing(false); }, 1000);
        });
    }

    private void refreshAllData() {
        loadRecentActivity();
        loadTodaysMedications();
    }

    private void observeVitals() {
        if (elderUid == null) return;

        // OPTIMIZED: Use ONLY latest_vitals_entry document (1 listener instead of 5)
        // This reduces Firebase reads by 80% on this screen
        vitalsListener = db.collection("users").document(elderUid).collection("vitals")
                .document("latest_vitals_entry")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;
                    if (doc != null && doc.exists() && binding != null && isAdded()) {
                        updateVitalsUI(doc);
                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts != null) {
                            binding.tvLastUpdated.setText("Last updated: " + dateFormat.format(ts.toDate()));
                            binding.tvLastUpdated.setVisibility(View.VISIBLE);
                        }
                    }
                });
        ListenerRegistry.INSTANCE.register("caregiver_overview_vitals", vitalsListener);
        
        // REMOVED: Individual vital listeners (heartRate, systolicBP, diastolicBP, bloodGlucose)
        // These were causing 4 extra real-time listeners = 4x the Firebase reads
        // All data now comes from single latest_vitals_entry document
    }


    private void updateVitalsUI(DocumentSnapshot doc) {
        if (doc.contains("heartRate")) {
            Number hr = doc.getLong("heartRate");
            if (hr != null) {
                binding.layoutHeartRate.tvVitalValue.setText(String.valueOf(hr.intValue()));
                binding.layoutHeartRate.vStatusDot.setBackgroundColor(VitalsClassifier.getColorForHeartRate(hr.intValue()));
            }
        }
        if (doc.contains("steps")) {
            Number steps = doc.getLong("steps");
            if (steps != null) binding.layoutSteps.tvVitalValue.setText(String.valueOf(steps.intValue()));
        }
        if (doc.contains("systolicBP") && doc.contains("diastolicBP")) {
            Number sys = doc.getLong("systolicBP");
            Number dia = doc.getLong("diastolicBP");
            if (sys != null && dia != null) {
                binding.layoutBP.tvVitalValue.setText(sys.intValue() + "/" + dia.intValue());
                binding.layoutBP.vStatusDot.setBackgroundColor(VitalsClassifier.getColorForBP(sys.intValue()));
            }
        }
        if (doc.contains("bloodGlucose")) {
            Number gluc = doc.getDouble("bloodGlucose");
            if (gluc != null) {
                binding.layoutGlucose.tvVitalValue.setText(String.format(Locale.getDefault(), "%.1f", gluc.doubleValue()));
                binding.layoutGlucose.vStatusDot.setBackgroundColor(VitalsClassifier.getColorForGlucose(gluc.doubleValue()));
            }
        }
        if (doc.contains("sleepHours")) {
            Number sleep = doc.getDouble("sleepHours");
            if (sleep != null) {
                binding.layoutSleep.tvVitalValue.setText(String.format(Locale.getDefault(), "%.1f", sleep.doubleValue()));
                binding.layoutSleep.vStatusDot.setBackgroundColor(VitalsClassifier.getColorForSleep(sleep.doubleValue()));
            }
        }
    }

    private void observeLiveStatus() {
        if (elderUid == null) return;
        alertsListener = db.collection("users").document(elderUid).collection("alerts")
                .whereEqualTo("isResolved", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    int alertCount = value.size();
                    if (alertCount == 0) {
                        binding.cardLiveStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                        binding.ivStatusIcon.setImageResource(R.drawable.baseline_check_circle_24);
                        binding.ivStatusIcon.clearAnimation();
                        binding.tvStatusPrimary.setText("All Vitals Normal");
                        binding.tvStatusSecondary.setText("No active alerts");
                        binding.btnViewAlerts.setVisibility(View.GONE);
                    } else {
                        binding.cardLiveStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                        binding.ivStatusIcon.setImageResource(R.drawable.baseline_warning_24);
                        startPulsingAnimation(binding.ivStatusIcon);
                        DocumentSnapshot latestAlert = value.getDocuments().get(0);
                        binding.tvStatusPrimary.setText(latestAlert.getString("title") != null ? latestAlert.getString("title") : "Active Health Alert");
                        binding.tvStatusSecondary.setText(latestAlert.getString("message") != null ? latestAlert.getString("message") : alertCount + " unresolved alert(s)");
                        binding.btnViewAlerts.setVisibility(View.VISIBLE);
                    }
                });
        ListenerRegistry.INSTANCE.register("caregiver_overview_alerts", alertsListener);
    }

    private void observeMedicationAdherence() {
        if (elderUid == null) return;
        medsListener = db.collection("users").document(elderUid).collection("medications")
                .whereEqualTo("isActive", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    int totalMeds = value.size();
                    int takenCount = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        Boolean taken = doc.getBoolean("takenToday");
                        if (Boolean.TRUE.equals(taken)) takenCount++;
                    }
                    if (totalMeds > 0) {
                        binding.cardMedsAdherence.setVisibility(View.VISIBLE);
                        binding.tvMedsCount.setText(takenCount + " of " + totalMeds + " medications taken today");
                        binding.pbMedsAdherence.setProgress((int) (((float) takenCount / totalMeds) * 100));
                    } else {
                        binding.cardMedsAdherence.setVisibility(View.GONE);
                    }
                });
        ListenerRegistry.INSTANCE.register("caregiver_overview_meds", medsListener);
    }

    private void loadRecentActivity() {
        if (elderUid == null) return;
        
        db.collection("users").document(elderUid).collection("vitals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(docs -> {
                    if (binding == null || !isAdded()) return;
                    List<Map<String, Object>> activityList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : docs) {
                        String type = doc.getString("type");
                        String docId = doc.getId();
                        
                        if (type != null && !type.isEmpty() 
                                && !type.equals("vitals_entry")
                                && !docId.startsWith("latest_")) {
                            activityList.add(doc.getData());
                        }
                    }
                    activityAdapter.updateData(activityList);
                });
    }

    private void loadTodaysMedications() {
        if (elderUid == null) return;
        
        long todayStart = getTodayStartTimestamp();
        
        db.collection("users").document(elderUid).collection("medications")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(docs -> {
                    if (binding == null || !isAdded()) return;
                    
                    List<Medication> todaysMeds = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : docs) {
                        Medication med = doc.toObject(Medication.class);
                        if (med != null) {
                            med.setId(doc.getId());
                            
                            // Check if medication is scheduled for today
                            if (isMedicationForToday(med)) {
                                todaysMeds.add(med);
                            }
                        }
                    }
                    
                    medsAdapter.updateData(todaysMeds);
                    
                    if (todaysMeds.isEmpty()) {
                        binding.rvMedications.setVisibility(View.GONE);
                    } else {
                        binding.rvMedications.setVisibility(View.VISIBLE);
                    }
                });
    }
    
    private boolean isMedicationForToday(Medication med) {
        // If medication has times list, it's scheduled for today
        if (med.getTimes() != null && !med.getTimes().isEmpty()) {
            return true;
        }
        
        // If medication has a single time, it's for today
        if (med.getTime() != null && !med.getTime().isEmpty()) {
            return true;
        }
        
        // If medication has frequency, check if it should be taken today
        String frequency = med.getFrequency();
        if (frequency != null) {
            // For daily medications
            if (frequency.toLowerCase().contains("daily") || 
                frequency.toLowerCase().contains("every day")) {
                return true;
            }
            
            // For as-needed medications
            if (frequency.toLowerCase().contains("as needed") || 
                frequency.toLowerCase().contains("prn")) {
                return true;
            }
        }
        
        // Default to showing if no clear schedule
        return true;
    }
    
    private long getTodayStartTimestamp() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void startPulsingAnimation(View view) {
        ScaleAnimation anim = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(800);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        view.startAnimation(anim);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ListenerRegistry.INSTANCE.unregister("caregiver_overview_vitals");
        ListenerRegistry.INSTANCE.unregister("caregiver_overview_alerts");
        ListenerRegistry.INSTANCE.unregister("caregiver_overview_meds");
        binding = null;
    }
}
