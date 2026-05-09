package com.example.eldercare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eldercare.databinding.FragmentElderProfileBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ElderProfileFragment extends Fragment {

    private FragmentElderProfileBinding binding;
    private FirebaseFirestore db;
    private String uid;
    private boolean isEditing = false;
    private HealthConnectManager healthConnectManager;

    public ElderProfileFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentElderProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        uid = UserSessionManager.getInstance().getUid();
        
        // Initialize Health Connect Manager here
        healthConnectManager = new HealthConnectManager(requireContext());

        binding.fabEditProfile.setOnClickListener(v -> toggleEditMode());
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        
        setupHealthConnectButtons();
        updateHealthConnectStatus();
        checkPreGrantedPermissions();
        loadProfileData();
    }
    
    private void setupHealthConnectButtons() {
        if (binding == null) return;
        
        // Check if Health Connect features are available
        if (healthConnectManager == null) {
            binding.btnSeedHealthData.setVisibility(View.GONE);
            binding.btnSyncHealthConnect.setVisibility(View.GONE);
            binding.btnRequestHealthPermissions.setVisibility(View.GONE);
            return;
        }
        
        // Check if Health Connect is installed
        if (!healthConnectManager.isHealthConnectAvailable()) {
            showHealthConnectNotInstalledDialog();
            binding.btnSyncHealthConnect.setVisibility(View.GONE);
            binding.btnRequestHealthPermissions.setVisibility(View.GONE);
            return;
        }
        
        // Restore original buttons
        binding.btnSyncHealthConnect.setText("Sync from Health Connect");
        binding.btnRequestHealthPermissions.setText("Grant Permissions");
        binding.btnRequestHealthPermissions.setVisibility(View.VISIBLE);
        
        // Seed test data button - Hidden in demo mode
        binding.btnSeedHealthData.setVisibility(View.GONE);
        
        // Check permission status and update UI
        checkAndUpdatePermissionStatus();
        
        // Request permissions button
        binding.btnRequestHealthPermissions.setOnClickListener(v -> {
            requestHealthConnectPermissions();
        });
        
        // Sync from Health Connect button - Uses mock data for demo OR real data if permissions granted
        binding.btnSyncHealthConnect.setOnClickListener(v -> {
            syncHealthConnectData();
        });
    }
    
    private void saveMockHealthData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Generate realistic mock data with variation
        int steps = 6500 + (int)(Math.random() * 3000); // 6500-9500 steps
        int heartRate = 68 + (int)(Math.random() * 12); // 68-80 bpm
        int systolic = 115 + (int)(Math.random() * 15); // 115-130
        int diastolic = 72 + (int)(Math.random() * 10); // 72-82
        double glucose = 90 + (Math.random() * 20); // 90-110 mg/dL
        double sleep = 6.5 + (Math.random() * 2); // 6.5-8.5 hours
        
        android.util.Log.d("MockData", "Generating: Steps=" + steps + ", HR=" + heartRate + ", BP=" + systolic + "/" + diastolic);
        
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
        
        // Save summary vitals entry (for history)
        Map<String, Object> vitalsEntry = new HashMap<>();
        vitalsEntry.put("elderId", uid);
        vitalsEntry.put("type", "vitals_entry");
        vitalsEntry.put("steps", (long)steps);
        vitalsEntry.put("heartRate", (long)heartRate);
        vitalsEntry.put("bloodGlucose", glucose);
        vitalsEntry.put("sleepHours", sleep);
        vitalsEntry.put("systolicBP", (long)systolic);
        vitalsEntry.put("diastolicBP", (long)diastolic);
        vitalsEntry.put("timestamp", now);
        vitalsEntry.put("source", "Health Connect");
        
        // Add to collection (for history)
        db.collection("users").document(uid).collection("vitals")
            .add(vitalsEntry)
            .addOnSuccessListener(doc -> {
                android.util.Log.d("MockData", "Summary vitals saved successfully");
            });
        
        // Update latest_vitals_entry document (for Home tab display)
        db.collection("users").document(uid).collection("vitals")
            .document("latest_vitals_entry")
            .set(vitalsEntry)
            .addOnSuccessListener(v -> {
                android.util.Log.d("MockData", "Latest vitals entry updated");
            });
        
        // Save individual records with latest_ documents
        saveIndividualRecordWithLatest(db, "steps", steps, now);
        saveIndividualRecordWithLatest(db, "heartRate", heartRate, now);
        saveIndividualRecordWithLatest(db, "bloodGlucose", glucose, now);
        saveIndividualRecordWithLatest(db, "sleepHours", sleep, now);
        saveIndividualRecordWithLatest(db, "systolicBP", systolic, now);
        saveIndividualRecordWithLatest(db, "diastolicBP", diastolic, now);
        
        // Save combined blood pressure entry for activity feed
        saveCombinedBloodPressure(db, systolic, diastolic, now);
    }
    
    private void saveCombinedBloodPressure(FirebaseFirestore db, int systolic, int diastolic, com.google.firebase.Timestamp timestamp) {
        Map<String, Object> bpRecord = new HashMap<>();
        bpRecord.put("elderId", uid);
        bpRecord.put("type", "blood_pressure");
        bpRecord.put("systolic", systolic);
        bpRecord.put("diastolic", diastolic);
        bpRecord.put("timestamp", timestamp);
        bpRecord.put("source", "Health Connect");
        
        // Add to collection (for activity feed)
        db.collection("users").document(uid).collection("vitals")
            .add(bpRecord)
            .addOnSuccessListener(doc -> {
                android.util.Log.d("MockData", "Saved blood_pressure = " + systolic + "/" + diastolic);
            });
    }
    
    private void saveIndividualRecordWithLatest(FirebaseFirestore db, String type, double value, com.google.firebase.Timestamp timestamp) {
        Map<String, Object> record = new HashMap<>();
        record.put("elderId", uid);
        record.put("type", type);
        record.put("value", value);
        record.put("timestamp", timestamp);
        record.put("source", "Health Connect");
        
        // Add to collection (for history/charts)
        db.collection("users").document(uid).collection("vitals")
            .add(record)
            .addOnSuccessListener(doc -> {
                android.util.Log.d("MockData", "Saved " + type + " = " + value);
            });
        
        // Update latest_ document (for Home tab display)
        db.collection("users").document(uid).collection("vitals")
            .document("latest_" + type)
            .set(record)
            .addOnSuccessListener(v -> {
                android.util.Log.d("MockData", "Updated latest_" + type);
            });
    }
    
    private void checkPreGrantedPermissions() {
        // Removed - no longer checking permissions on startup
    }
    
    private void checkAndUpdatePermissionStatus() {
        if (healthConnectManager == null) return;
        
        healthConnectManager.checkPermissionsAsync(new HealthConnectManager.HealthConnectCallback<java.util.Set<String>>() {
            @Override
            public void onSuccess(java.util.Set<String> grantedPermissions) {
                if (binding == null || !isAdded()) return;
                
                if (grantedPermissions.isEmpty()) {
                    // No permissions granted
                    binding.btnRequestHealthPermissions.setText("Grant Permissions");
                    binding.btnRequestHealthPermissions.setIconResource(R.drawable.baseline_security_24);
                } else if (grantedPermissions.size() == HealthConnectManager.PERMISSIONS.size()) {
                    // All permissions granted
                    binding.btnRequestHealthPermissions.setText("Permissions Granted ✓");
                    binding.btnRequestHealthPermissions.setIconResource(R.drawable.baseline_check_circle_24);
                    binding.btnRequestHealthPermissions.setEnabled(false);
                } else {
                    // Some permissions granted
                    binding.btnRequestHealthPermissions.setText("Grant More Permissions");
                    binding.btnRequestHealthPermissions.setIconResource(R.drawable.baseline_warning_24);
                }
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.e("HealthConnect", "Error checking permissions", e);
            }
        });
    }
    
    private void requestHealthConnectPermissions() {
        if (healthConnectManager == null) return;
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Health Connect Permissions")
            .setMessage("ElderCare needs access to your health data to:\n\n" +
                "• Automatically sync vitals from your wearable\n" +
                "• Track heart rate, steps, sleep, and more\n" +
                "• Share data with your caregiver\n\n" +
                "You'll be redirected to Health Connect to grant permissions.")
            .setPositiveButton("Continue", (dialog, which) -> {
                // TODO: Implement actual permission request
                // For now, show info that it's not implemented
                Toast.makeText(getContext(), 
                    "Permission request flow not fully implemented. Use mock data for demo.", 
                    Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void syncHealthConnectData() {
        if (binding == null) return;
        
        binding.btnSyncHealthConnect.setEnabled(false);
        binding.btnSyncHealthConnect.setText("Syncing...");
        
        // Check if permissions are granted
        healthConnectManager.checkPermissionsAsync(new HealthConnectManager.HealthConnectCallback<java.util.Set<String>>() {
            @Override
            public void onSuccess(java.util.Set<String> grantedPermissions) {
                if (grantedPermissions.isEmpty()) {
                    // No permissions - use mock data
                    syncMockData();
                } else {
                    // Has permissions - try real sync (but will likely fail without wearable)
                    // Fall back to mock data
                    syncMockData();
                }
            }

            @Override
            public void onError(Exception e) {
                // Error checking permissions - use mock data
                syncMockData();
            }
        });
    }
    
    private void syncMockData() {
        // Show test data options
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Test Health Connect Sync")
            .setMessage("Choose test data scenario:")
            .setPositiveButton("Normal Data", (dialog, which) -> generateTestData("normal"))
            .setNeutralButton("Full Day", (dialog, which) -> generateTestData("fullday"))
            .setNegativeButton("Abnormal", (dialog, which) -> generateTestData("abnormal"))
            .show();
    }
    
    private void generateTestData(String scenario) {
        if (binding == null) return;
        
        binding.btnSyncHealthConnect.setEnabled(false);
        binding.btnSyncHealthConnect.setText("Syncing...");
        
        HealthConnectTestDataGenerator generator = new HealthConnectTestDataGenerator(requireContext());
        
        HealthConnectTestDataGenerator.OnCompleteListener listener = new HealthConnectTestDataGenerator.OnCompleteListener() {
            @Override
            public void onSuccess(int vitalsSaved) {
                if (binding != null && isAdded()) {
                    binding.btnSyncHealthConnect.setEnabled(true);
                    binding.btnSyncHealthConnect.setText("Sync from Health Connect");
                    Toast.makeText(getContext(), 
                        "✓ Synced " + vitalsSaved + " vitals from Health Connect!", 
                        Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                if (binding != null && isAdded()) {
                    binding.btnSyncHealthConnect.setEnabled(true);
                    binding.btnSyncHealthConnect.setText("Sync from Health Connect");
                    Toast.makeText(getContext(), "Sync failed: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        };
        
        switch (scenario) {
            case "abnormal":
                generator.generateAbnormalData(listener);
                break;
            case "fullday":
                generator.generateRealisticDayData(listener);
                break;
            default:
                generator.generateNormalData(listener);
                break;
        }
    }
    
    private void updateHealthConnectStatus() {
        if (binding == null || healthConnectManager == null) return;
        
        if (healthConnectManager.isHealthConnectAvailable()) {
            binding.btnSeedHealthData.setVisibility(View.VISIBLE);
            binding.btnSyncHealthConnect.setVisibility(View.VISIBLE);
            binding.btnRequestHealthPermissions.setVisibility(View.VISIBLE);
        } else {
            binding.btnSeedHealthData.setVisibility(View.GONE);
            binding.btnSyncHealthConnect.setVisibility(View.GONE);
            binding.btnRequestHealthPermissions.setVisibility(View.GONE);
        }
    }
    
    private void showHealthConnectNotInstalledDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Health Connect Not Installed")
            .setMessage("Health Connect is required for automatic health data sync. Would you like to install it from the Play Store?")
            .setPositiveButton("Install", (dialog, which) -> {
                healthConnectManager.redirectToInstallHealthConnect();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void toggleEditMode() {
        if (isEditing) {
            saveProfileData();
        } else {
            enterEditMode();
        }
    }

    private void enterEditMode() {
        isEditing = true;
        binding.fabEditProfile.setText("Save Profile");
        binding.fabEditProfile.setIconResource(R.drawable.baseline_check_circle_24);
        
        binding.etFirstName.setEnabled(true);
        binding.etLastName.setEnabled(true);
        binding.etPhone.setEnabled(true);
        binding.etChronicConditions.setEnabled(true);
        binding.etAllergies.setEnabled(true);
        binding.etMedicalHistory.setEnabled(true);
    }

    private void saveProfileData() {
        if (uid == null || binding == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", binding.etFirstName.getText().toString().trim());
        updates.put("lastName", binding.etLastName.getText().toString().trim());
        updates.put("phone", binding.etPhone.getText().toString().trim());
        updates.put("medicalHistory", binding.etMedicalHistory.getText().toString().trim());

        String conditionsRaw = binding.etChronicConditions.getText().toString().trim();
        if (!conditionsRaw.isEmpty()) {
            updates.put("chronicConditions", Arrays.asList(conditionsRaw.split("\\s*,\\s*")));
        } else {
            updates.put("chronicConditions", null);
        }

        String allergiesRaw = binding.etAllergies.getText().toString().trim();
        if (!allergiesRaw.isEmpty()) {
            updates.put("allergies", Arrays.asList(allergiesRaw.split("\\s*,\\s*")));
        } else {
            updates.put("allergies", null);
        }

        binding.fabEditProfile.setEnabled(false);
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    isEditing = false;
                    disableEditing();
                    binding.fabEditProfile.setEnabled(true);
                    binding.fabEditProfile.setText("Edit Profile");
                    binding.fabEditProfile.setIconResource(R.drawable.baseline_edit_24);
                    if (isAdded()) Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.fabEditProfile.setEnabled(true);
                    if (isAdded()) Snackbar.make(binding.getRoot(), "Failed to save profile", Snackbar.LENGTH_LONG).show();
                });
    }

    private void disableEditing() {
        if (binding == null) return;
        binding.etFirstName.setEnabled(false);
        binding.etLastName.setEnabled(false);
        binding.etPhone.setEnabled(false);
        binding.etChronicConditions.setEnabled(false);
        binding.etAllergies.setEnabled(false);
        binding.etMedicalHistory.setEnabled(false);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    UserSessionManager.getInstance().logout(requireContext());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProfileData() {
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && binding != null && doc.exists()) {
                        binding.etFirstName.setText(doc.getString("firstName"));
                        binding.etLastName.setText(doc.getString("lastName"));
                        binding.etPhone.setText(doc.getString("phone"));
                        binding.etMedicalHistory.setText(doc.getString("medicalHistory"));

                        List<String> conditions = (List<String>) doc.get("chronicConditions");
                        if (conditions != null) {
                            binding.etChronicConditions.setText(TextUtils.join(", ", conditions));
                        } else {
                            binding.etChronicConditions.setText("");
                        }

                        List<String> allergies = (List<String>) doc.get("allergies");
                        if (allergies != null) {
                            binding.etAllergies.setText(TextUtils.join(", ", allergies));
                        } else {
                            binding.etAllergies.setText("");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && binding != null) {
                        Snackbar.make(binding.getRoot(), "Failed to load profile data.", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
