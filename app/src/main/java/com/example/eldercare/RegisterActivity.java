package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "REGISTER_FLOW";
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword, etAge, etPhone, etInviteCode;
    private View layoutCaregiverLink;
    private RadioGroup rgUserType;
    private MaterialButton btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private View progressBar;
    private LinkingCodeManager codeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        codeManager = new LinkingCodeManager();
        UserSessionManager.getInstance().init(this);

        initViews();
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etRegisterFirstName);
        etLastName = findViewById(R.id.etRegisterLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAge = findViewById(R.id.etRegisterAge);
        etPhone = findViewById(R.id.etRegisterPhone);
        etInviteCode = findViewById(R.id.etInviteCode);
        layoutCaregiverLink = findViewById(R.id.layoutCaregiverLink);
        rgUserType = findViewById(R.id.rgUserType);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCaregiver) {
                layoutCaregiverLink.setVisibility(View.VISIBLE);
            } else {
                layoutCaregiverLink.setVisibility(View.GONE);
            }
        });

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        
        boolean isCaregiver = rgUserType.getCheckedRadioButtonId() == R.id.rbCaregiver;
        String role = isCaregiver ? "caregiver" : "elder";

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(ageStr)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        performRegistration(firstName, lastName, email, password, ageStr, phone, role);
    }

    private void performRegistration(String firstName, String lastName, String email, String password, String age, String phone, String role) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getUid();
                        saveUserAndHandleLinking(uid, firstName, lastName, email, age, phone, role);
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            showError("Account already exists with this email.");
                        } else {
                            showError("Auth Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                        }
                    }
                });
    }

    private void saveUserAndHandleLinking(String uid, String firstName, String lastName, String email, String age, String phone, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email.toLowerCase().trim());
        user.put("age", age);
        user.put("phone", phone);
        user.put("role", role);
        user.put("createdAt", FieldValue.serverTimestamp());
        
        // Set default step goal for elders
        if ("elder".equals(role)) {
            user.put("stepGoal", 5000);
        }

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    String inviteCode = etInviteCode.getText().toString().trim();
                    if ("caregiver".equals(role) && !TextUtils.isEmpty(inviteCode)) {
                        codeManager.claimCode(inviteCode, uid, new LinkingCodeManager.Callback<String>() {
                            @Override
                            public void onSuccess(String linkedElderId) {
                                // Save linked elder ID to local session
                                UserSessionManager.getInstance().setSession(RegisterActivity.this, uid, role, firstName, lastName, linkedElderId, null);
                                navigateToDashboard(role);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                // Save profile anyway, but without elder link
                                UserSessionManager.getInstance().setSession(RegisterActivity.this, uid, role, firstName, lastName, null, null);
                                Toast.makeText(RegisterActivity.this, "Linked failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                navigateToDashboard(role);
                            }
                        });
                    } else {
                        UserSessionManager.getInstance().setSession(this, uid, role, firstName, lastName, null, null);
                        navigateToDashboard(role);
                    }
                })
                .addOnFailureListener(e -> showError("Firestore Error: " + e.getMessage()));
    }

    private void navigateToDashboard(String role) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(this, "caregiver".equals(role) ? CaregiverMainActivity.class : ElderMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnRegister.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }
}
