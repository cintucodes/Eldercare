package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eldercare.databinding.ActivityLoginBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isNavigating = false; // Fix 2: Flag to prevent duplicate navigation/finish

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        UserSessionManager.getInstance().init(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserRoleAndNavigate(currentUser.getUid());
        }

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        binding.ivLogo.setOnLongClickListener(v -> {
            injectDemoData();
            return true;
        });
    }

    private void injectDemoData() {
        String uid = mAuth.getUid();
        if (uid == null) {
            Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Generating consistent mock data...", Toast.LENGTH_SHORT).show();
        SecureRandom random = new SecureRandom();

        String[] types = {"steps", "heartRate", "bloodGlucose", "systolicBP"};
        for (String type : types) {
            for (int i = 0; i < 15; i++) {
                Map<String, Object> record = new HashMap<>();
                record.put("elderId", uid);
                record.put("type", type);
                
                long offset = i * 2 * 3600000L;
                record.put("timestamp", new Timestamp(new java.util.Date(System.currentTimeMillis() - offset)));
                
                if (type.equals("systolicBP")) record.put("value", (double)(110 + random.nextInt(30)));
                else if (type.equals("steps")) record.put("value", (double)(100 + random.nextInt(500)));
                else if (type.equals("bloodGlucose")) record.put("value", 4.5 + random.nextDouble() * 3.0);
                else record.put("value", (double)(65 + random.nextInt(25)));
                
                db.collection("users").document(uid).collection("vitals").add(record);
            }
        }

        Toast.makeText(this, "Demo Data Injected!", Toast.LENGTH_LONG).show();
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Please fill all fields", Snackbar.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        fetchUserRoleAndNavigate(mAuth.getUid());
                    } else {
                        setLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Auth Failed";
                        Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        EditText resetMail = new EditText(this);
        resetMail.setHint("Enter your email");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password").setView(resetMail)
                .setPositiveButton("Send", (dialog, which) -> {
                    String mail = resetMail.getText().toString().trim();
                    if (!TextUtils.isEmpty(mail)) {
                        mAuth.sendPasswordResetEmail(mail).addOnSuccessListener(aVoid -> 
                            Toast.makeText(this, "Reset link sent.", Toast.LENGTH_SHORT).show()
                        );
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void fetchUserRoleAndNavigate(String uid) {
        if (isNavigating) return;
        setLoading(true);
        
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (!doc.exists()) {
                            mAuth.signOut();
                            setLoading(false);
                            return;
                        }

                        // Suggested Improvement 1: Role Verification
                        String role = doc.getString("role");
                        if (role == null) {
                            Toast.makeText(this, "Account role not configured", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            setLoading(false);
                            return;
                        }

                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");
                        String linkedElderId = doc.getString("linkedElderId");
                        String linkedCaregiverId = doc.getString("linkedCaregiverId");

                        UserSessionManager.getInstance().clearSession(this);
                        UserSessionManager.getInstance().setSession(this, uid, role, firstName, lastName, linkedElderId, linkedCaregiverId);

                        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                            db.collection("users").document(uid).update("fcmToken", token);
                        });

                        if ("elder".equals(role)) {
                            fetchLinkedPhoneAndNavigate(linkedCaregiverId, "elder");
                        } else if ("caregiver".equals(role)) {
                            fetchLinkedPhoneAndNavigate(linkedElderId, "caregiver");
                        } else {
                            Toast.makeText(this, "Unsupported role: " + role, Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            setLoading(false);
                        }
                    } else {
                        setLoading(false);
                    }
                });
    }

    private void fetchLinkedPhoneAndNavigate(String linkedId, String role) {
        if (linkedId == null) {
            navigateBasedOnRole(role);
            return;
        }
        db.collection("users").document(linkedId).get()
                .addOnSuccessListener(doc -> {
                    String phone = doc.getString("phone");
                    UserSessionManager.getInstance().setLinkedPhone(this, phone);
                    navigateBasedOnRole(role);
                })
                .addOnFailureListener(e -> navigateBasedOnRole(role));
    }

    private void navigateBasedOnRole(String role) {
        if (isNavigating) return;
        isNavigating = true;
        
        setLoading(false);
        Intent intent = "caregiver".equals(role) ? 
                new Intent(this, CaregiverMainActivity.class) : 
                new Intent(this, ElderMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Fix 4: Release resources or stop animations if any were running
    }
}
