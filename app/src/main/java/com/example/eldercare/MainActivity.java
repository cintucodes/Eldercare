package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Schedule SyncWorker
        scheduleSync();
        
        // Root activity that redirects to Login or Dashboard
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            fetchUserRoleAndNavigate(user.getUid());
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void fetchUserRoleAndNavigate(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String role = task.getResult().getString("role");
                        String firstName = task.getResult().getString("firstName");
                        String lastName = task.getResult().getString("lastName");
                        String linkedElderId = task.getResult().getString("linkedElderId");
                        String linkedCaregiverId = task.getResult().getString("linkedCaregiverId");

                        // Refresh session to prevent data bleed
                        UserSessionManager.getInstance().clearSession(this);
                        UserSessionManager.getInstance().setSession(this, uid, role, firstName, lastName, linkedElderId, linkedCaregiverId);

                        String linkedId = "elder".equals(role) ? linkedCaregiverId : linkedElderId;
                        fetchLinkedPhoneAndNavigate(linkedId, role);
                    } else {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                });
    }

    private void fetchLinkedPhoneAndNavigate(String linkedId, String role) {
        if (linkedId == null) {
            navigateBasedOnRole(role);
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(linkedId).get()
                .addOnSuccessListener(doc -> {
                    String phone = doc.getString("phone");
                    UserSessionManager.getInstance().setLinkedPhone(this, phone);
                    navigateBasedOnRole(role);
                })
                .addOnFailureListener(e -> navigateBasedOnRole(role));
    }

    private void navigateBasedOnRole(String role) {
        Intent intent = "caregiver".equals(role) ? 
                new Intent(this, CaregiverMainActivity.class) : 
                new Intent(this, ElderMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ElderCareSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}
