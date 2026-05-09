package com.example.eldercare;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MedicationActivity handles adding and displaying medications.
 */
public class MedicationActivity extends AppCompatActivity {

    private TextInputEditText etMedName, etDosage, etFrequency, etTime;
    private MaterialButton btnAddMed;
    private RecyclerView rvMedications;
    private MedicationAdapter adapter;
    private List<Medication> medicationList;
    private FirebaseFirestore db;
    private String userId;
    private View progressBar;
    private ListenerRegistration medListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupRecyclerView();
        setupTimePicker();
        
        btnAddMed.setOnClickListener(v -> saveMedication());
        
        loadMedications();
    }

    private void initViews() {
        etMedName = findViewById(R.id.etMedName);
        etDosage = findViewById(R.id.etDosage);
        etFrequency = findViewById(R.id.etFrequency);
        etTime = findViewById(R.id.etMedTime);
        btnAddMed = findViewById(R.id.btnAddMedication);
        rvMedications = findViewById(R.id.rvMedications);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        medicationList = new ArrayList<>();
        adapter = new MedicationAdapter(medicationList, true, this::deleteMedication);
        rvMedications.setLayoutManager(new LinearLayoutManager(this));
        rvMedications.setAdapter(adapter);
    }

    private void setupTimePicker() {
        etTime.setFocusable(false);
        etTime.setOnClickListener(v -> {
            java.util.Calendar mcurrentTime = java.util.Calendar.getInstance();
            int hour = mcurrentTime.get(java.util.Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(java.util.Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
            }, hour, minute, true);
            mTimePicker.show();
        });
    }

    private void saveMedication() {
        if (userId == null) return;
        
        String name = etMedName.getText().toString().trim();
        String dosage = etDosage.getText().toString().trim();
        String freq = etFrequency.getText().toString().trim();
        String time = etTime.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(dosage) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> med = new HashMap<>();
        med.put("name", name);
        med.put("dosage", dosage);
        med.put("frequency", freq);
        med.put("time", time);
        med.put("timestamp", Timestamp.now());

        db.collection("users").document(userId).collection("medications")
                .add(med)
                .addOnSuccessListener(documentReference -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.med_added, Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMedications() {
        if (userId == null) return;

        if (medListener != null) {
            medListener.remove();
            ListenerRegistry.INSTANCE.unregister("medication_activity_" + userId);
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        medListener = db.collection("users").document(userId).collection("medications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Error loading meds", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        medicationList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Medication med = doc.toObject(Medication.class);
                            if (med != null) {
                                med.setId(doc.getId());
                                medicationList.add(med);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
        
        ListenerRegistry.INSTANCE.register("medication_activity_" + userId, medListener);
    }

    private void deleteMedication(Medication med) {
        if (userId == null) return;
        db.collection("users").document(userId).collection("medications").document(med.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show());
    }

    private void clearFields() {
        etMedName.setText("");
        etDosage.setText("");
        etFrequency.setText("");
        etTime.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (medListener != null) {
            medListener.remove();
            if (userId != null) ListenerRegistry.INSTANCE.unregister("medication_activity_" + userId);
        }
    }
}
