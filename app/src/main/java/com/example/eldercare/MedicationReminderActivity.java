package com.example.eldercare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;

import com.example.eldercare.databinding.ActivityMedicationReminderBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class MedicationReminderActivity extends AppCompatActivity {

    private ActivityMedicationReminderBinding binding;
    private String medId, medName, dosage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicationReminderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        medId = getIntent().getStringExtra("medId");
        medName = getIntent().getStringExtra("medName");
        dosage = getIntent().getStringExtra("dosage");

        binding.tvMedName.setText(medName);
        binding.tvMedDosage.setText("Dosage: " + dosage);

        binding.btnMarkTaken.setOnClickListener(v -> markAsTaken());
        binding.btnSnooze.setOnClickListener(v -> snooze());
    }

    private void markAsTaken() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || medId == null) return;

        // Cancel the missed medication worker
        WorkManager.getInstance(this).cancelAllWorkByTag("missed_" + medId);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("medications").document(medId)
                .update("status", "taken", "taken_at", Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medication marked as taken", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void snooze() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MedicationReminderReceiver.class);
        intent.putExtra("medId", medId);
        intent.putExtra("medName", medName);
        intent.putExtra("dosage", dosage);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, medId.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long snoozeTime = System.currentTimeMillis() + (15 * 60 * 1000);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
        }

        Toast.makeText(this, "Snoozed for 15 minutes", Toast.LENGTH_SHORT).show();
        finish();
    }
}
