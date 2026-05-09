package com.example.eldercare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AddAppointmentActivity extends AppCompatActivity {

    private TextInputEditText etDoctorName, etDate, etTime, etLocation, etPhone, etNotes;
    private Spinner spinnerSpecialty;
    private MaterialButton btnSave;
    private Calendar calendar;
    private FirebaseFirestore db;
    private String userId;
    private String targetElderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_appointment);

        db = FirebaseFirestore.getInstance();
        UserSessionManager session = UserSessionManager.getInstance();
        session.init(this);
        
        userId = session.getUid();
        if (session.isCaregiver()) {
            targetElderId = session.getLinkedElderId();
        } else {
            targetElderId = userId;
        }

        calendar = Calendar.getInstance();

        setupToolbar();
        initViews();
        setupDateTimePickers();
        setupSpecialtySpinner();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Appointment");
        }
    }

    private void initViews() {
        etDoctorName = findViewById(R.id.etApptDoctorName);
        etDate = findViewById(R.id.etApptDate);
        etTime = findViewById(R.id.etApptTime);
        etLocation = findViewById(R.id.etApptLocation);
        etPhone = findViewById(R.id.etApptPhone);
        etNotes = findViewById(R.id.etApptNotes);
        spinnerSpecialty = findViewById(R.id.spinnerApptSpecialty);
        btnSave = findViewById(R.id.btnSaveAppointment);

        btnSave.setOnClickListener(v -> saveAppointment());
    }

    private void setupDateTimePickers() {
        etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });
    }

    private void setupSpecialtySpinner() {
        String[] specialties = {"General Practitioner", "Cardiologist", "Neurologist", "Orthopedic", "Dentist", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, specialties);
        spinnerSpecialty.setAdapter(adapter);
    }

    private void saveAppointment() {
        String doctorName = etDoctorName.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (doctorName.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetElderId == null) {
            Toast.makeText(this, "Error: No elder linked to this account", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("doctorName", doctorName);
        appointment.put("specialty", spinnerSpecialty.getSelectedItem().toString());
        appointment.put("dateTime", new Timestamp(calendar.getTime()));
        appointment.put("location", location);
        appointment.put("doctorPhone", etPhone.getText().toString().trim());
        appointment.put("notes", etNotes.getText().toString().trim());
        appointment.put("reminderEnabled", true);

        db.collection("users").document(targetElderId).collection("appointments")
                .add(appointment)
                .addOnSuccessListener(documentReference -> {
                    scheduleWorkManagerReminders(documentReference.getId(), doctorName, date + " " + time);
                    Toast.makeText(this, "Appointment saved for elder", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void scheduleWorkManagerReminders(String appointmentId, String doctor, String time) {
        long apptTimeMillis = calendar.getTimeInMillis();
        long now = System.currentTimeMillis();

        // 24h Reminder
        long delay24h = (apptTimeMillis - (24 * 60 * 60 * 1000)) - now;
        if (delay24h > 0) {
            enqueueReminder(appointmentId, doctor, time, "24h", delay24h);
        }

        // 1h Reminder
        long delay1h = (apptTimeMillis - (60 * 60 * 1000)) - now;
        if (delay1h > 0) {
            enqueueReminder(appointmentId, doctor, time, "1h", delay1h);
        }
    }

    private void enqueueReminder(String id, String doctor, String time, String type, long delay) {
        Data inputData = new Data.Builder()
                .putString("appointmentId", id)
                .putString("doctorName", doctor)
                .putString("time", time)
                .putString("reminderType", type)
                .build();

        OneTimeWorkRequest reminderRequest = new OneTimeWorkRequest.Builder(AppointmentReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("appt_" + id + "_" + type)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(reminderRequest);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
