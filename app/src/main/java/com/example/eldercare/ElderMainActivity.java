package com.example.eldercare;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.eldercare.databinding.ActivityElderMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ElderMainActivity extends AppCompatActivity {

    private static final String TAG = "ElderMainActivity";
    private ActivityElderMainBinding binding;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Log.d(TAG, "Permissions processed");
                checkFullScreenIntentPermission();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Bug 6 Fix: SplashScreen API
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        
        UserSessionManager.getInstance().init(this);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityElderMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        requestInitialPermissions();
        // Health Connect permissions moved to Profile tab - no longer auto-requested on login
        startMonitoringService(); // Fix: Session timeout prevention

        setSupportActionBar(binding.toolbar);
        updateSubtitle();

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            replaceFragment(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            binding.getRoot().post(() -> {
                if (getIntent().getBooleanExtra("openAppointments", false)) {
                    binding.bottomNavigation.setSelectedItemId(R.id.nav_appointments);
                } else if (getIntent().getBooleanExtra("openProfileTab", false)) {
                    binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
                } else {
                    replaceFragment(R.id.nav_home);
                }
            });
        }
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, ElderMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateSubtitle() {
        if (getSupportActionBar() != null) {
            String name = UserSessionManager.getInstance().getFirstName();
            if (name != null) {
                getSupportActionBar().setSubtitle("Welcome, " + name);
            } else {
                // Fallback to Firestore if session is not fully populated yet
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                        if (doc.exists() && binding != null) {
                            String fName = doc.getString("firstName");
                            String lName = doc.getString("lastName");
                            // Update session too
                            UserSessionManager.getInstance().updateName(this, fName, lName);
                            getSupportActionBar().setSubtitle("Welcome, " + (fName != null ? fName : ""));
                        }
                    });
                }
            }
        }
    }

    private void checkFullScreenIntentPermission() {
        // Bug 4 Requirement 2: API 34+ runtime check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && !nm.canUseFullScreenIntent()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Full Screen Alerts")
                        .setMessage("ElderCare needs permission to show full-screen alerts for critical medication reminders and emergencies.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        })
                        .setNegativeButton("Not Now", null)
                        .show();
            }
        }
    }

    private void replaceFragment(int itemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        
        if (currentFragment != null) {
            if (itemId == R.id.nav_home && currentFragment instanceof ElderHomeFragment) return;
            if (itemId == R.id.nav_vitals && currentFragment instanceof LogVitalsFragment) return;
            if (itemId == R.id.nav_meds && currentFragment instanceof MedicationsFragment) return;
            if (itemId == R.id.nav_appointments && currentFragment instanceof AppointmentsFragment) return;
            if (itemId == R.id.nav_profile && currentFragment instanceof ElderProfileFragment) return;
        }

        Fragment selectedFragment = null;
        String title = "Home";

        if (itemId == R.id.nav_home) {
            selectedFragment = new ElderHomeFragment();
            title = "Home";
        } else if (itemId == R.id.nav_vitals) {
            selectedFragment = new LogVitalsFragment();
            title = "Log Vitals";
        } else if (itemId == R.id.nav_meds) {
            selectedFragment = new MedicationsFragment();
            title = "Medications";
        } else if (itemId == R.id.nav_appointments) {
            selectedFragment = new AppointmentsFragment();
            title = "Appointments";
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ElderProfileFragment();
            title = "Profile";
        }

        if (selectedFragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .setReorderingAllowed(true)
                    .commit();
            
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
        }
    }

    private void requestInitialPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        permissionLauncher.launch(permissions.toArray(new String[0]));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    UserSessionManager.getInstance().logout(this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Fix 4: Release resources or stop animations/renders if any
    }
}
