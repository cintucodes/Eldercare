package com.example.eldercare;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * VitalsActivity allows users to log their health metrics with real-time validation.
 */
public class VitalsActivity extends AppCompatActivity {

    private TextInputEditText etHeartRate, etSystolic, etDiastolic, etGlucose, etTemp, etWeight, etOxygen, etNotes;
    private TextInputLayout tilSystolic, tilDiastolic, tilGlucose, tilOxygen;
    private MaterialButton btnSave;
    private View progressBar;
    private FirebaseFirestore db;
    private String userId;
    private ElderCareDatabase localDb;

    private boolean isSystolicValid = false;
    private boolean isDiastolicValid = false;
    private boolean isGlucoseValid = true;
    private boolean isOxygenValid = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vitals);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        
        if (userId == null) {
            finish();
            return;
        }

        localDb = ElderCareDatabase.getInstance(this);

        initViews();
        setupValidators();
        btnSave.setOnClickListener(v -> saveVitals());
        updateSaveButtonState();
    }

    private void initViews() {
        etHeartRate = findViewById(R.id.etHeartRate);
        etSystolic = findViewById(R.id.etSystolic);
        etDiastolic = findViewById(R.id.etDiastolic);
        etGlucose = findViewById(R.id.etGlucose);
        etTemp = findViewById(R.id.etTemp);
        etWeight = findViewById(R.id.etWeight);
        etOxygen = findViewById(R.id.etOxygen); // Corrected ID reference
        etNotes = findViewById(R.id.etVitalsNotes);
        btnSave = findViewById(R.id.btnSaveVitals);
        progressBar = findViewById(R.id.progressBar);

        // Safely find parent TextInputLayouts
        tilSystolic = findParentTextInputLayout(etSystolic);
        tilDiastolic = findParentTextInputLayout(etDiastolic);
        tilGlucose = findParentTextInputLayout(etGlucose);
        tilOxygen = findParentTextInputLayout(etOxygen);
    }

    private TextInputLayout findParentTextInputLayout(View view) {
        if (view == null) return null;
        View parent = (View) view.getParent();
        while (parent != null) {
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }
            parent = (View) parent.getParent();
        }
        return null;
    }

    private void setupValidators() {
        if (etSystolic != null && tilSystolic != null) {
            etSystolic.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    VitalsValidator.ValidationResult result = VitalsValidator.validateSystolic(s.toString());
                    tilSystolic.setError(result.isValid ? null : result.errorMessage);
                    isSystolicValid = result.isValid;
                    updateSaveButtonState();
                }
            });
        }

        if (etDiastolic != null && tilDiastolic != null) {
            etDiastolic.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    VitalsValidator.ValidationResult result = VitalsValidator.validateDiastolic(s.toString());
                    tilDiastolic.setError(result.isValid ? null : result.errorMessage);
                    isDiastolicValid = result.isValid;
                    updateSaveButtonState();
                }
            });
        }

        if (etGlucose != null && tilGlucose != null) {
            etGlucose.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (TextUtils.isEmpty(s)) {
                        tilGlucose.setError(null);
                        isGlucoseValid = true;
                    } else {
                        VitalsValidator.ValidationResult result = VitalsValidator.validateGlucose(s.toString());
                        tilGlucose.setError(result.isValid ? null : result.errorMessage);
                        isGlucoseValid = result.isValid;
                    }
                    updateSaveButtonState();
                }
            });
        }

        if (etOxygen != null && tilOxygen != null) {
            etOxygen.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (TextUtils.isEmpty(s)) {
                        tilOxygen.setError(null);
                        isOxygenValid = true;
                    } else {
                        try {
                            int oxy = Integer.parseInt(s.toString());
                            if (oxy < 70 || oxy > 100) {
                                tilOxygen.setError("Oxygen must be 70-100%");
                                isOxygenValid = false;
                            } else {
                                tilOxygen.setError(null);
                                isOxygenValid = true;
                            }
                        } catch (NumberFormatException e) {
                            tilOxygen.setError("Invalid number");
                            isOxygenValid = false;
                        }
                    }
                    updateSaveButtonState();
                }
            });
        }
    }

    private void updateSaveButtonState() {
        if (btnSave != null) {
            btnSave.setEnabled(isSystolicValid && isDiastolicValid && isGlucoseValid && isOxygenValid);
        }
    }

    private void saveVitals() {
        if (userId == null) return;

        try {
            int hr = TextUtils.isEmpty(etHeartRate.getText()) ? 0 : Integer.parseInt(etHeartRate.getText().toString());
            int sys = Integer.parseInt(etSystolic.getText().toString());
            int dia = Integer.parseInt(etDiastolic.getText().toString());
            double glucose = TextUtils.isEmpty(etGlucose.getText()) ? 0 : Double.parseDouble(etGlucose.getText().toString());
            double temp = TextUtils.isEmpty(etTemp.getText()) ? 0 : Double.parseDouble(etTemp.getText().toString());
            double weight = TextUtils.isEmpty(etWeight.getText()) ? 0 : Double.parseDouble(etWeight.getText().toString());
            int oxygen = TextUtils.isEmpty(etOxygen.getText()) ? 0 : Integer.parseInt(etOxygen.getText().toString());
            String notes = etNotes != null ? etNotes.getText().toString() : "";
            
            Timestamp now = Timestamp.now();

            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);

            VitalsClassifier.VitalsStatus bpStatus = VitalsClassifier.classifyBloodPressure(sys, dia);
            VitalsClassifier.VitalsStatus glucoseStatus = glucose > 0 ? VitalsClassifier.classifyGlucose(glucose) : null;

            VitalEntity vitalEntity = new VitalEntity(userId, hr, sys + "/" + dia, glucose, now.getSeconds() * 1000);
            
            Executors.newSingleThreadExecutor().execute(() -> {
                localDb.vitalDao().insert(vitalEntity);
                
                Map<String, Object> vitalData = new HashMap<>();
                vitalData.put("heartRate", hr);
                vitalData.put("systolicBP", sys);
                vitalData.put("diastolicBP", dia);
                vitalData.put("bloodGlucose", glucose);
                vitalData.put("bloodOxygen", oxygen);
                vitalData.put("bpStatus", bpStatus.label);
                vitalData.put("glucoseStatus", glucoseStatus != null ? glucoseStatus.label : "N/A");
                vitalData.put("temperature", temp);
                vitalData.put("weight", weight);
                vitalData.put("notes", notes);
                vitalData.put("recordedAt", now);
                vitalData.put("timestamp", now.getSeconds() * 1000);
                vitalData.put("source", "manual");

                db.collection("users").document(userId).collection("vitals")
                        .add(vitalData)
                        .addOnSuccessListener(ref -> {
                            vitalEntity.syncedToFirestore = true;
                            Executors.newSingleThreadExecutor().execute(() -> localDb.vitalDao().update(vitalEntity));
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Vitals saved manually", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                btnSave.setEnabled(true);
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Saved offline", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        });
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please check your inputs", Toast.LENGTH_SHORT).show();
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
