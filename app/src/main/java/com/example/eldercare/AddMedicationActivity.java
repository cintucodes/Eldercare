package com.example.eldercare;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddMedicationActivity extends AppCompatActivity {

    private TextInputEditText etMedName, etDosage, etInstructions;
    private Spinner spinnerType, spinnerFrequency;
    private LinearLayout layoutTimePickers;
    private SwitchMaterial switchReminder;
    private MaterialButton btnSave;
    
    private List<String> selectedTimes = new ArrayList<>();
    private ElderCareDatabase localDb;
    private String targetElderId;

    private TimePickerDialog timePickerDialog;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        UserSessionManager session = UserSessionManager.getInstance();
        session.init(this);
        
        if (session.isCaregiver()) {
            targetElderId = session.getLinkedElderId();
        } else {
            targetElderId = session.getUid();
        }

        localDb = ElderCareDatabase.getInstance(this);

        setupToolbar();
        initViews();
        setupSpinners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Medication");
        }
    }

    private void initViews() {
        etMedName = findViewById(R.id.etMedName);
        etDosage = findViewById(R.id.etDosage);
        etInstructions = findViewById(R.id.etInstructions);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        layoutTimePickers = findViewById(R.id.layoutTimePickers);
        switchReminder = findViewById(R.id.switchReminder);
        btnSave = findViewById(R.id.btnSaveMedication);

        btnSave.setOnClickListener(v -> saveMedication());
    }

    private void setupSpinners() {
        String[] types = {"Tablet", "Capsule", "Liquid", "Injection", "Other"};
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        String[] frequencies = {"Once Daily", "Twice Daily", "Three Times Daily", "As Needed"};
        spinnerFrequency.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, frequencies));

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTimePickers(frequencies[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTimePickers(String frequency) {
        layoutTimePickers.removeAllViews();
        selectedTimes.clear();
        int count = 0;
        if (frequency.equals("Once Daily")) count = 1;
        else if (frequency.equals("Twice Daily")) count = 2;
        else if (frequency.equals("Three Times Daily")) count = 3;

        for (int i = 0; i < count; i++) {
            final int index = i;
            selectedTimes.add("08:00");
            MaterialButton btnTime = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
            btnTime.setText("Set Time " + (i + 1) + ": 08:00");
            btnTime.setOnClickListener(v -> {
                Calendar mcurrentTime = Calendar.getInstance();
                if (timePickerDialog != null && timePickerDialog.isShowing()) timePickerDialog.dismiss();
                
                timePickerDialog = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    selectedTimes.set(index, time);
                    btnTime.setText("Set Time " + (index + 1) + ": " + time);
                }, mcurrentTime.get(Calendar.HOUR_OF_DAY), mcurrentTime.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            });
            layoutTimePickers.addView(btnTime);
        }
    }

    private void saveMedication() {
        String name = etMedName.getText().toString();
        String dosage = etDosage.getText().toString();
        if (name.isEmpty() || dosage.isEmpty() || targetElderId == null) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStr = selectedTimes.isEmpty() ? "" : selectedTimes.get(0);
        MedicationEntity medEntity = new MedicationEntity(targetElderId, name, dosage, timeStr);

        diskExecutor.execute(() -> {
            localDb.medicationDao().insert(medEntity);

            Map<String, Object> med = new HashMap<>();
            med.put("name", name);
            med.put("dosage", dosage);
            med.put("type", spinnerType.getSelectedItem().toString());
            med.put("frequency", spinnerFrequency.getSelectedItem().toString());
            med.put("time", timeStr);
            med.put("times", selectedTimes);
            med.put("startDate", Timestamp.now());
            med.put("remindersEnabled", switchReminder.isChecked());
            med.put("isActive", true);

            FirebaseFirestore.getInstance().collection("users").document(targetElderId).collection("medications").add(med)
                    .addOnSuccessListener(ref -> {
                        if (switchReminder.isChecked()) {
                            MedicationReminderScheduler.scheduleAlarms(this, ref.getId(), name, dosage, selectedTimes);
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timePickerDialog != null) {
            timePickerDialog.dismiss();
            timePickerDialog = null;
        }
        diskExecutor.shutdown(); // Fix Bug 7: Prevent executor leak
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
