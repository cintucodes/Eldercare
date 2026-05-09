package com.example.eldercare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eldercare.databinding.FragmentElderHomeBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ElderHomeFragment extends Fragment {

    private static final String TAG = "ElderHomeFragment";
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int DEFAULT_STEP_GOAL = 5000;
    private FragmentElderHomeBinding binding;
    private FirebaseFirestore db;
    private String uid;
    private int stepGoal = DEFAULT_STEP_GOAL;
    private CountDownTimer sosTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentElderHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) return;

        setupUI();
        loadData();
        loadLinkedCaregivers();
        observeVitals();
    }

    private void setupUI() {
        String timeGreeting = getTimeGreeting();
        binding.tvGreeting.setText(timeGreeting + "...");

        binding.btnSOS.setOnClickListener(v -> {
            if (checkSOSPermissions()) {
                showSOSCountdown();
            } else {
                requestSOSPermissions();
            }
        });
        
        binding.btnLinkCaregiver.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ElderInviteActivity.class));
        });

        binding.cardCaregiverConnection.setOnClickListener(v -> {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        List<String> linkedCaregivers = (List<String>) doc.get("linkedCaregivers");
                        if (linkedCaregivers != null && !linkedCaregivers.isEmpty()) {
                            showCaregiverListDialog();
                        } else {
                            startActivity(new Intent(getContext(), ElderInviteActivity.class));
                        }
                    });
        });

        binding.layoutHeartRate.tvVitalLabel.setText("Heart Rate");
        binding.layoutHeartRate.tvVitalUnit.setText("bpm");
        binding.layoutHeartRate.tvVitalValue.setText("--");
        
        binding.layoutBP.tvVitalLabel.setText("Blood Pressure");
        binding.layoutBP.tvVitalUnit.setText("mmHg");
        binding.layoutBP.tvVitalValue.setText("--/--");
        
        binding.layoutGlucose.tvVitalLabel.setText("Glucose");
        binding.layoutGlucose.tvVitalUnit.setText("mmol/L");
        binding.layoutGlucose.tvVitalValue.setText("--");
        
        binding.layoutSleep.tvVitalLabel.setText("Sleep");
        binding.layoutSleep.tvVitalUnit.setText("hrs");
        binding.layoutSleep.tvVitalValue.setText("--");
    }

    private boolean checkSOSPermissions() {
        boolean hasBasicPermissions = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        // For Android 10+, also check background location
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return hasBasicPermissions && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        
        return hasBasicPermissions;
    }

    private void requestSOSPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSOSCountdown();
            } else {
                Snackbar.make(binding.getRoot(), "Permissions required for SOS SMS and location.", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private String getTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private void loadData() {
        if (uid == null) return;

        // Run migration for existing caregivers (one-time update)
        new CaregiverSubcollectionMigration().updateCaregiverSubcollections(uid, (updated, failed) -> {
            if (updated > 0) {
                Log.d(TAG, "Migrated " + updated + " caregiver subcollections");
            }
        });

        ListenerRegistration userListener = db.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists() || binding == null || !isAdded()) return;
            
            String firstName = doc.getString("firstName");
            binding.tvGreeting.setText(getTimeGreeting() + ", " + (firstName != null ? firstName : ""));
            
            // Load custom step goal
            Long goalFromDb = doc.getLong("stepGoal");
            stepGoal = (goalFromDb != null && goalFromDb > 0) ? goalFromDb.intValue() : DEFAULT_STEP_GOAL;
            
            updateCaregiverConnection(doc);
            updateStepDisplay(); // Refresh step display with new goal
        });
        ListenerRegistry.INSTANCE.register("elder_home_user", userListener);
    }

    private void updateCaregiverConnection(DocumentSnapshot elderDoc) {
        List<String> linkedCaregivers = (List<String>) elderDoc.get("linkedCaregivers");
        
        Log.d(TAG, "updateCaregiverConnection - linkedCaregivers: " + 
            (linkedCaregivers != null ? linkedCaregivers.size() + " caregivers" : "null"));
        
        if (linkedCaregivers != null && !linkedCaregivers.isEmpty()) {
            binding.tvCaregiverName.setText("Linked Caregivers (" + linkedCaregivers.size() + ")");
            binding.chipConnectionStatus.setText("Connected");
            binding.chipConnectionStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
            binding.btnLinkCaregiver.setVisibility(View.GONE);
            
            Log.d(TAG, "Caregivers found, hiding Link Caregiver button");
            loadCaregiverSummary(linkedCaregivers.size());
        } else {
            binding.tvCaregiverName.setText("No Caregiver Linked");
            binding.chipConnectionStatus.setText("Not Connected");
            binding.chipConnectionStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
            binding.tvConnectionInstruction.setText("Tap to generate a code and link a caregiver.");
            binding.btnLinkCaregiver.setVisibility(View.VISIBLE);
            
            Log.d(TAG, "No caregivers found, showing Link Caregiver button");
        }
    }
    
    private void loadCaregiverSummary(int count) {
        if (uid == null || binding == null) return;
        
        binding.tvConnectionInstruction.setText("Tap to view " + count + " linked caregiver" + (count > 1 ? "s" : "") + " and their contact info.");
    }

    private void loadLinkedCaregivers() {
        // This method is kept for potential future use but main display is handled by updateCaregiverConnection
    }

    private void observeVitals() {
        if (uid == null) return;

        ListenerRegistration summaryListener = db.collection("users").document(uid).collection("vitals")
                .document("latest_vitals_entry")
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists() && binding != null && isAdded()) {
                        updateUIFromSummary(doc);
                    }
                });
        ListenerRegistry.INSTANCE.register("elder_home_summary", summaryListener);

        observeIndividualVital("heartRate");
        observeIndividualVital("steps");
        observeIndividualVital("bloodGlucose");
        observeIndividualVital("systolicBP");
        observeIndividualVital("diastolicBP");
        observeIndividualVital("sleepHours");
    }

    private void observeIndividualVital(String type) {
        ListenerRegistration listener = db.collection("users").document(uid).collection("vitals")
                .document("latest_" + type)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || binding == null || !isAdded() || doc == null || !doc.exists()) return;
                    
                    Object valObj = doc.get("value");
                    if (valObj instanceof Number) {
                        double val = ((Number) valObj).doubleValue();
                        switch (type) {
                            case "heartRate":
                                binding.layoutHeartRate.tvVitalValue.setText(String.valueOf((int)val));
                                binding.layoutHeartRate.dotStatus.setBackgroundColor(VitalsClassifier.getColorForHeartRate((int)val));
                                break;
                            case "steps":
                                binding.tvStepCount.setText((int)val + " / " + stepGoal);
                                binding.pbStepGoal.setProgress((int) Math.min(100, (val * 100.0 / stepGoal)));
                                break;
                            case "bloodGlucose":
                                binding.layoutGlucose.tvVitalValue.setText(String.format(Locale.getDefault(), "%.1f", val));
                                binding.layoutGlucose.dotStatus.setBackgroundColor(VitalsClassifier.getColorForGlucose(val));
                                break;
                            case "systolicBP":
                                updateBloodPressureFromVital("systolic", (int)val);
                                break;
                            case "diastolicBP":
                                updateBloodPressureFromVital("diastolic", (int)val);
                                break;
                            case "sleepHours":
                                binding.layoutSleep.tvVitalValue.setText(String.format(Locale.getDefault(), "%.1f", val));
                                binding.layoutSleep.dotStatus.setBackgroundColor(VitalsClassifier.getColorForSleep(val));
                                break;
                        }
                        updateSummaryText();
                    }
                });
        ListenerRegistry.INSTANCE.register("elder_home_vital_" + type, listener);
    }

    private void updateBloodPressureFromVital(String type, int value) {
        if (binding == null || uid == null) return;
        
        String currentText = binding.layoutBP.tvVitalValue.getText().toString();
        String[] parts = currentText.split("/");
        
        int systolic = 0;
        int diastolic = 0;
        
        if (parts.length == 2) {
            try {
                systolic = Integer.parseInt(parts[0].trim().replace("--", "0"));
                diastolic = Integer.parseInt(parts[1].trim().replace("--", "0"));
            } catch (NumberFormatException e) {
                // Keep as 0
            }
        }
        
        if (type.equals("systolic")) {
            systolic = value;
        } else {
            diastolic = value;
        }
        
        if (systolic > 0 && diastolic > 0) {
            binding.layoutBP.tvVitalValue.setText(systolic + "/" + diastolic);
            binding.layoutBP.dotStatus.setBackgroundColor(VitalsClassifier.getColorForBP(systolic));
        } else if (systolic > 0) {
            binding.layoutBP.tvVitalValue.setText(systolic + "/--");
        } else if (diastolic > 0) {
            binding.layoutBP.tvVitalValue.setText("--/" + diastolic);
        }
    }

    private void updateUIFromSummary(DocumentSnapshot doc) {
        if (doc.contains("steps")) {
            Number steps = doc.getLong("steps");
            if (steps != null) {
                binding.tvStepCount.setText(steps.intValue() + " / " + stepGoal);
                binding.pbStepGoal.setProgress((int) Math.min(100, (steps.doubleValue() * 100.0 / stepGoal)));
            }
        }
        if (doc.contains("heartRate")) {
            Number hr = doc.getLong("heartRate");
            if (hr != null) {
                binding.layoutHeartRate.tvVitalValue.setText(String.valueOf(hr.intValue()));
                binding.layoutHeartRate.dotStatus.setBackgroundColor(VitalsClassifier.getColorForHeartRate(hr.intValue()));
            }
        }
        if (doc.contains("systolicBP") && doc.contains("diastolicBP")) {
            Number sys = doc.getLong("systolicBP");
            Number dia = doc.getLong("diastolicBP");
            if (sys != null && dia != null) {
                binding.layoutBP.tvVitalValue.setText(sys.intValue() + "/" + dia.intValue());
                binding.layoutBP.dotStatus.setBackgroundColor(VitalsClassifier.getColorForBP(sys.intValue()));
            }
        }
        if (doc.contains("bloodGlucose")) {
            Number gluc = doc.getDouble("bloodGlucose");
            if (gluc != null) {
                binding.layoutGlucose.tvVitalValue.setText(String.format(Locale.getDefault(), "%.1f", gluc.doubleValue()));
                binding.layoutGlucose.dotStatus.setBackgroundColor(VitalsClassifier.getColorForGlucose(gluc.doubleValue()));
            }
        }
        if (doc.contains("sleepHours")) {
            Number sleep = doc.getDouble("sleepHours");
            if (sleep != null) {
                binding.layoutSleep.tvVitalValue.setText(String.format(Locale.getDefault(), "%.1f", sleep.doubleValue()));
                binding.layoutSleep.dotStatus.setBackgroundColor(VitalsClassifier.getColorForSleep(sleep.doubleValue()));
            }
        }
        updateSummaryText();
    }

    private void updateSummaryText() {
        if (binding == null) return;
        String hr = binding.layoutHeartRate.tvVitalValue.getText().toString();
        String steps = binding.tvStepCount.getText().toString().split(" / ")[0];
        binding.tvSummaryVitals.setText("Last HR: " + hr + " bpm | Today: " + steps + " steps");
    }
    
    private void updateStepDisplay() {
        // Refresh step count display with current goal
        if (binding == null || uid == null) return;
        db.collection("users").document(uid).collection("vitals")
            .document("latest_steps")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc != null && doc.exists() && binding != null) {
                    Object valObj = doc.get("value");
                    if (valObj instanceof Number) {
                        int steps = ((Number) valObj).intValue();
                        binding.tvStepCount.setText(steps + " / " + stepGoal);
                        binding.pbStepGoal.setProgress((int) Math.min(100, (steps * 100.0 / stepGoal)));
                    }
                }
            });
    }

    private void showSOSCountdown() {
        // Check if location services are enabled
        SOSManager sosManager = SOSManager.getInstance(requireContext());
        if (!sosManager.isLocationEnabled()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Location Services Disabled")
                    .setMessage("Please enable location services (GPS or Network) to send accurate SOS alerts with your location.")
                    .setPositiveButton("Enable", (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Send Anyway", (dialog, which) -> proceedWithSOSCountdown())
                    .setNeutralButton("Cancel", null)
                    .show();
            return;
        }
        
        proceedWithSOSCountdown();
    }
    
    private void proceedWithSOSCountdown() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sos_countdown, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);
        androidx.appcompat.app.AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        android.widget.TextView tvTimer = dialogView.findViewById(R.id.tvCountdown);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancelSOS);

        sosTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Sending SOS in " + (millisUntilFinished / 1000 + 1) + "...");
            }
            @Override
            public void onFinish() {
                dialog.dismiss();
                triggerSOS();
            }
        }.start();

        btnCancel.setOnClickListener(v -> {
            if (sosTimer != null) sosTimer.cancel();
            dialog.dismiss();
        });
    }

    private void triggerSOS() {
        if (binding == null || !isAdded()) return;
        SOSManager.getInstance(requireContext()).triggerSOS(uid);
        Snackbar.make(binding.getRoot(), "SOS Alert Sent!", Snackbar.LENGTH_LONG).show();
    }

    private void showCaregiverListDialog() {
        // Open full-screen elder-friendly activity
        Intent intent = new Intent(getContext(), ElderCaregiversActivity.class);
        startActivity(intent);
    }
    
    private void showRelinkDialog() {
        if (!isAdded()) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Re-link Required")
                .setMessage("Your caregiver link needs to be updated. Please:\n\n" +
                        "1. Generate a new linking code\n" +
                        "2. Have your caregiver enter the code\n\n" +
                        "This is a one-time update to enable new features.")
                .setPositiveButton("Generate Code", (dialog, which) -> 
                    startActivity(new Intent(getContext(), ElderInviteActivity.class)))
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showCaregiverDetailsDialog(List<CaregiverInfo> caregivers) {
        if (!isAdded()) return;

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < caregivers.size(); i++) {
            CaregiverInfo cg = caregivers.get(i);
            if (i > 0) message.append("\n\n");
            message.append("👤 ").append(cg.name).append("\n");
            message.append("Relationship: ").append(cg.relationship).append("\n");
            message.append("📞 ").append(cg.phone != null ? cg.phone : "No phone").append("\n");
            if (cg.email != null && !cg.email.isEmpty()) {
                message.append("📧 ").append(cg.email);
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Linked Caregivers (" + caregivers.size() + ")")
                .setMessage(message.toString())
                .setPositiveButton("Call", (dialog, which) -> showCallSelectionDialog(caregivers))
                .setNegativeButton("Close", null)
                .setNeutralButton("Generate New Code", (dialog, which) -> 
                    startActivity(new Intent(getContext(), ElderInviteActivity.class)))
                .show();
    }

    private void showCallSelectionDialog(List<CaregiverInfo> caregivers) {
        if (!isAdded()) return;

        String[] items = new String[caregivers.size()];
        for (int i = 0; i < caregivers.size(); i++) {
            CaregiverInfo cg = caregivers.get(i);
            items[i] = cg.name + " (" + cg.relationship + ")";
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Caregiver to Call")
                .setItems(items, (dialog, which) -> {
                    String phone = caregivers.get(which).phone;
                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } else {
                        Toast.makeText(getContext(), "No phone number available", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class CaregiverInfo {
        String name;
        String phone;
        String relationship;
        String email;

        CaregiverInfo(String name, String phone, String relationship, String email) {
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
            this.email = email;
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sosTimer != null) sosTimer.cancel();
        ListenerRegistry.INSTANCE.unregister("elder_home_user");
        ListenerRegistry.INSTANCE.unregister("elder_home_summary");
        ListenerRegistry.INSTANCE.unregister("elder_home_vital_heartRate");
        ListenerRegistry.INSTANCE.unregister("elder_home_vital_steps");
        ListenerRegistry.INSTANCE.unregister("elder_home_vital_bloodGlucose");
        ListenerRegistry.INSTANCE.unregister("elder_home_vital_systolicBP");
        ListenerRegistry.INSTANCE.unregister("elder_home_vital_diastolicBP");
        ListenerRegistry.INSTANCE.unregister("elder_home_vital_sleepHours");
        binding = null;
    }
}
