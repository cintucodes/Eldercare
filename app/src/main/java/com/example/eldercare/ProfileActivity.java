package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private TextView tvName, tvAge;
    private MaterialButton btnLogout, btnAction;
    private FirebaseFirestore db;
    private String userId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Security Check: Ensure user is authenticated
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user found, redirecting to Login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        userId = user.getUid();

        initViews();
        loadUserProfile();
        setupBottomNavigation(R.id.nav_profile);
        
        // HIDDEN TRIGGER FOR EMULATOR/DEBUG: 
        // Long press the name to open Developer Settings
        if (BuildConfig.DEBUG && tvName != null) {
            tvName.setOnLongClickListener(v -> {
                startActivity(new Intent(this, DeveloperSettingsActivity.class));
                return true;
            });
        }
    }

    private void initViews() {
        tvName = findViewById(R.id.tvProfileName);
        tvAge = findViewById(R.id.tvProfileAge);
        btnLogout = findViewById(R.id.btnSaveChanges); 
        btnAction = findViewById(R.id.btnEditProfile); 
        
        if (btnLogout != null) {
            btnLogout.setText("Logout");
            btnLogout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.alert_red)));
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadUserProfile() {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("firstName");
                        if (name == null) name = documentSnapshot.getString("name"); // Fallback

                        String age = documentSnapshot.getString("age");
                        userRole = documentSnapshot.getString("role");
                        String linkedId = documentSnapshot.getString("linkedElderId");
                        
                        if (name != null) tvName.setText(name);
                        if (age != null) tvAge.setText("Age: " + age);

                        setupActionButton(userRole, linkedId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading profile", e));
    }

    private void setupActionButton(String role, String linkedId) {
        if (btnAction == null) return;

        if ("elder".equals(role)) {
            btnAction.setText("Invite Caregiver");
            btnAction.setOnClickListener(v -> {
                startActivity(new Intent(this, ElderInviteActivity.class));
            });
        } else if ("caregiver".equals(role)) {
            if (linkedId != null && !linkedId.isEmpty()) {
                btnAction.setText("Linked to Elder");
                btnAction.setEnabled(false);
            } else {
                btnAction.setText("Link to Elder");
                btnAction.setOnClickListener(v -> {
                    startActivity(new Intent(this, CaregiverLinkActivity.class));
                });
            }
        } else {
            btnAction.setText("Link Account");
            btnAction.setOnClickListener(v -> {
                startActivity(new Intent(this, CaregiverLinkActivity.class));
            });
        }
    }

    private void logout() {
        Log.d(TAG, "Logout initiated");
        UserSessionManager.getInstance().clearSession(this);
        db.clearPersistence().addOnCompleteListener(task -> {
            FirebaseAuth.getInstance().signOut();
            Log.d(TAG, "Sign out successful");
            
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
