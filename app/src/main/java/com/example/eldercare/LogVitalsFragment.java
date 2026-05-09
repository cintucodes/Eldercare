package com.example.eldercare;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eldercare.databinding.ActivityVitalsLoggingBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogVitalsFragment extends Fragment {

    private ActivityVitalsLoggingBinding binding;
    private FirebaseFirestore db;
    private String targetElderId;
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private ListenerRegistration summaryListener;

    private boolean isSystolicValid = false;
    private boolean isDiastolicValid = false;
    private boolean isGlucoseValid = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityVitalsLoggingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        UserSessionManager session = UserSessionManager.getInstance();
        session.init(requireContext());
        
        if (session.isCaregiver()) {
            targetElderId = session.getLinkedElderId();
        } else {
            targetElderId = session.getUid();
        }

        initTts();

        binding.npSleepHours.setMinValue(0);
        binding.npSleepHours.setMaxValue(24);
        binding.npSleepHours.setValue(8);

        binding.npSleepMinutes.setMinValue(0);
        binding.npSleepMinutes.setMaxValue(59);
        binding.npSleepMinutes.setValue(0);

        binding.tvTodayDate.setText(new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date()));
        
        setupValidators();
        binding.btnSaveVitals.setOnClickListener(v -> saveAllRecords());
        binding.btnUpdateStepGoal.setOnClickListener(v -> updateStepGoal());
        updateSaveButtonState();
        
        if (targetElderId != null) {
            fetchTodaysSummary();
            loadStepGoal();
        }
    }

    private void fetchTodaysSummary() {
        summaryListener = db.collection("users").document(targetElderId).collection("vitals")
                .document("latest_vitals_entry")
                .addSnapshotListener((doc, error) -> {
                    if (error != null || binding == null || !isAdded()) return;

                    if (doc == null || !doc.exists()) {
                        binding.tvNoVitalsPlaceholder.setVisibility(View.VISIBLE);
                        binding.layoutSummaryContent.setVisibility(View.GONE);
                    } else {
                        binding.tvNoVitalsPlaceholder.setVisibility(View.GONE);
                        binding.layoutSummaryContent.setVisibility(View.VISIBLE);
                        updateSummaryUI(doc);
                    }
                });
        ListenerRegistry.INSTANCE.register("log_vitals_summary", summaryListener);
    }

    private void updateSummaryUI(DocumentSnapshot doc) {
        Timestamp ts = doc.getTimestamp("timestamp");
        if (ts != null) {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts.toDate());
            binding.tvSummaryTimestamp.setText("Last updated: " + time);
        }

        // Heart Rate
        Number hr = doc.getLong("heartRate");
        if (hr != null && hr.intValue() > 0) {
            binding.tvSummaryHR.setText("HR: " + hr + " BPM");
            binding.tvSummaryHR.setVisibility(View.VISIBLE);
        } else {
            binding.tvSummaryHR.setText("HR: --");
        }

        // Blood Pressure
        Number sys = doc.getLong("systolicBP");
        Number dia = doc.getLong("diastolicBP");
        if (sys != null && dia != null && sys.intValue() > 0 && dia.intValue() > 0) {
            binding.tvSummaryBP.setText("BP: " + sys.intValue() + "/" + dia.intValue());
            binding.tvSummaryBP.setVisibility(View.VISIBLE);
        } else {
            binding.tvSummaryBP.setText("BP: --");
        }

        // Blood Glucose
        Double gluc = doc.getDouble("bloodGlucose");
        if (gluc != null && gluc > 0) {
            binding.tvSummaryGlucose.setText(String.format(Locale.getDefault(), "Glucose: %.1f", gluc));
            binding.tvSummaryGlucose.setVisibility(View.VISIBLE);
        } else {
            binding.tvSummaryGlucose.setText("Glucose: --");
        }

        // Steps
        Number steps = doc.getLong("steps");
        if (steps != null && steps.intValue() > 0) {
            binding.tvSummarySteps.setText("Steps: " + steps);
            binding.tvSummarySteps.setVisibility(View.VISIBLE);
        } else {
            binding.tvSummarySteps.setText("Steps: --");
        }

        // Sleep
        Double sleep = doc.getDouble("sleepHours");
        if (sleep != null && sleep > 0) {
            binding.tvSummarySleep.setText(String.format(Locale.getDefault(), "Sleep: %.1fh", sleep));
            binding.tvSummarySleep.setVisibility(View.VISIBLE);
        } else {
            binding.tvSummarySleep.setText("Sleep: --");
        }
    }

    private void initTts() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        isTtsReady = false;
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true;
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tts == null || !isTtsReady) {
            initTts();
        }
    }

    private void setupValidators() {
        binding.etSystolic.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    binding.tilSystolic.setError(null);
                    isSystolicValid = true;
                } else {
                    VitalsValidator.ValidationResult result = VitalsValidator.validateSystolic(s.toString());
                    binding.tilSystolic.setError(result.isValid ? null : result.errorMessage);
                    isSystolicValid = result.isValid;
                }
                updateSaveButtonState();
            }
        });

        binding.etDiastolic.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    binding.tilDiastolic.setError(null);
                    isDiastolicValid = true;
                } else {
                    VitalsValidator.ValidationResult result = VitalsValidator.validateDiastolic(s.toString());
                    binding.tilDiastolic.setError(result.isValid ? null : result.errorMessage);
                    isDiastolicValid = result.isValid;
                }
                updateSaveButtonState();
            }
        });

        binding.etGlucose.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    binding.tilGlucose.setError(null);
                    isGlucoseValid = true;
                } else {
                    VitalsValidator.ValidationResult result = VitalsValidator.validateGlucose(s.toString());
                    binding.tilGlucose.setError(result.isValid ? null : result.errorMessage);
                    isGlucoseValid = result.isValid;
                }
                updateSaveButtonState();
            }
        });
    }

    private void updateSaveButtonState() {
        binding.btnSaveVitals.setEnabled(isSystolicValid && isDiastolicValid && isGlucoseValid && targetElderId != null);
    }

    private void saveAllRecords() {
        if (targetElderId == null) {
            Toast.makeText(getContext(), "Error: No elder profile linked", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int hr = TextUtils.isEmpty(binding.etHeartRate.getText()) ? 0 : Integer.parseInt(binding.etHeartRate.getText().toString());
            int sys = TextUtils.isEmpty(binding.etSystolic.getText()) ? 0 : Integer.parseInt(binding.etSystolic.getText().toString());
            int dia = TextUtils.isEmpty(binding.etDiastolic.getText()) ? 0 : Integer.parseInt(binding.etDiastolic.getText().toString());
            double glucose = TextUtils.isEmpty(binding.etGlucose.getText()) ? 0 : Double.parseDouble(binding.etGlucose.getText().toString());
            int steps = TextUtils.isEmpty(binding.etSteps.getText()) ? 0 : Integer.parseInt(binding.etSteps.getText().toString());
            
            int sleepHrs = binding.npSleepHours.getValue();
            int sleepMins = binding.npSleepMinutes.getValue();
            double totalSleepDecimal = sleepHrs + (sleepMins / 60.0);

            Timestamp now = Timestamp.now();
            binding.btnSaveVitals.setEnabled(false);

            WriteBatch batch = db.batch();

            if (hr > 0) saveRecordToBatch(batch, targetElderId, "heartRate", (double) hr, now);
            if (sys > 0 && dia > 0) {
                saveRecordToBatch(batch, targetElderId, "systolicBP", (double) sys, now);
                saveRecordToBatch(batch, targetElderId, "diastolicBP", (double) dia, now);
            }
            if (glucose > 0) saveRecordToBatch(batch, targetElderId, "bloodGlucose", glucose, now);
            if (steps > 0) saveRecordToBatch(batch, targetElderId, "steps", (double) steps, now);
            saveRecordToBatch(batch, targetElderId, "sleepHours", totalSleepDecimal, now);

            Map<String, Object> bpRecord = new HashMap<>();
            bpRecord.put("elderId", targetElderId);
            bpRecord.put("type", "blood_pressure");
            bpRecord.put("systolic", sys);
            bpRecord.put("diastolic", dia);
            bpRecord.put("timestamp", now);
            batch.set(db.collection("users").document(targetElderId).collection("vitals").document(), bpRecord);

            Map<String, Object> vitalData = new HashMap<>();
            vitalData.put("elderId", targetElderId);
            vitalData.put("type", "vitals_entry");
            vitalData.put("heartRate", hr);
            vitalData.put("systolicBP", sys);
            vitalData.put("diastolicBP", dia);
            vitalData.put("bloodGlucose", glucose);
            vitalData.put("steps", steps);
            vitalData.put("sleepHours", totalSleepDecimal);
            vitalData.put("timestamp", now);

            batch.set(db.collection("users").document(targetElderId).collection("vitals").document(), vitalData);
            batch.set(db.collection("users").document(targetElderId).collection("vitals").document("latest_vitals_entry"), vitalData);

            batch.commit()
                    .addOnSuccessListener(ref -> {
                        if (isAdded()) {
                            speak("Vitals saved successfully");
                            Toast.makeText(getContext(), "Vitals saved", Toast.LENGTH_SHORT).show();
                            clearInputs();
                            binding.btnSaveVitals.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            binding.btnSaveVitals.setEnabled(true);
                            Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Check inputs", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearInputs() {
        binding.etHeartRate.setText("");
        binding.etSystolic.setText("");
        binding.etDiastolic.setText("");
        binding.etGlucose.setText("");
        binding.etSteps.setText("");
        binding.npSleepHours.setValue(8);
        binding.npSleepMinutes.setValue(0);
    }

    private void saveRecordToBatch(WriteBatch batch, String targetId, String type, double value, Timestamp timestamp) {
        Map<String, Object> record = new HashMap<>();
        record.put("elderId", targetId);
        record.put("type", type);
        record.put("value", value);
        record.put("timestamp", timestamp);
        
        batch.set(db.collection("users").document(targetId).collection("vitals").document(), record);
        batch.set(db.collection("users").document(targetId).collection("vitals").document("latest_" + type), record);
    }

    private void speak(String text) {
        if (tts != null && isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void loadStepGoal() {
        if (targetElderId == null) return;
        
        db.collection("users").document(targetElderId).get()
            .addOnSuccessListener(doc -> {
                if (binding != null && doc.exists()) {
                    Long stepGoal = doc.getLong("stepGoal");
                    binding.etStepGoal.setText(String.valueOf(stepGoal != null ? stepGoal : 5000));
                }
            });
    }

    private void updateStepGoal() {
        if (targetElderId == null || binding == null) return;
        
        String stepGoalStr = binding.etStepGoal.getText().toString().trim();
        if (stepGoalStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a step goal", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int stepGoal = Integer.parseInt(stepGoalStr);
            if (stepGoal < 1000) stepGoal = 1000;
            if (stepGoal > 50000) stepGoal = 50000;
            
            int finalStepGoal = stepGoal;
            db.collection("users").document(targetElderId)
                .update("stepGoal", stepGoal)
                .addOnSuccessListener(v -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Step goal updated to " + finalStepGoal, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to update step goal", Toast.LENGTH_SHORT).show();
                    }
                });
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) {
            if (isTtsReady) {
                tts.stop();
            }
            tts.shutdown();
            isTtsReady = false;
        }
        ListenerRegistry.INSTANCE.unregister("log_vitals_summary");
        binding = null;
    }
}
