package com.example.eldercare;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.eldercare.databinding.ActivityCaregiverMainBinding;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

public class CaregiverMainActivity extends AppCompatActivity {

    private static final String TAG = "CaregiverMainActivity";
    private ActivityCaregiverMainBinding binding;
    private FirebaseFirestore db;
    private String elderUid;
    private int currentPrimaryColor;
    private final int ALERT_RED = Color.parseColor("#C62828");
    private final int PRIMARY_TEAL = Color.parseColor("#00695C");
    private ObjectAnimator colorAnim; // Track animator for cleanup

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Log.d(TAG, "Permissions processed");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityCaregiverMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        UserSessionManager.getInstance().init(this);
        elderUid = UserSessionManager.getInstance().getLinkedElderId();
        currentPrimaryColor = PRIMARY_TEAL;

        setSupportActionBar(binding.toolbar);
        
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            replaceFragment(item.getItemId());
            return true;
        });
        
        if (elderUid != null) {
            fetchElderName();
            observeAlerts();
            setupFCM();
            startAlertService();
            requestNotificationPermission();
        } else {
            binding.toolbar.setSubtitle("No Elder Linked");
        }

        if (savedInstanceState == null) {
            binding.getRoot().post(() -> {
                int initialNav = getIntent().getBooleanExtra("open_alerts", false) ? R.id.nav_alerts : R.id.nav_overview;
                binding.bottomNavigation.setSelectedItemId(initialNav);
                replaceFragment(initialNav);
            });
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
        }
    }

    private void replaceFragment(int itemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);

        if (currentFragment != null) {
            if (itemId == R.id.nav_overview && currentFragment instanceof CaregiverOverviewFragment) return;
            if (itemId == R.id.nav_health_data && currentFragment instanceof ElderHealthDataFragment) return;
            if (itemId == R.id.nav_appointments && currentFragment instanceof AppointmentsFragment) return;
            if (itemId == R.id.nav_alerts && currentFragment instanceof AlertsFragment) return;
            if (itemId == R.id.nav_care_notes && currentFragment instanceof CareNotesFragment) return;
        }

        Fragment selectedFragment = null;
        String title = "Caregiver Dashboard";

        if (itemId == R.id.nav_overview) {
            selectedFragment = new CaregiverOverviewFragment();
            title = "Caregiver Dashboard";
        } else if (itemId == R.id.nav_health_data) {
            selectedFragment = new ElderHealthDataFragment();
            title = "Elder Health Data";
        } else if (itemId == R.id.nav_appointments) {
            selectedFragment = new AppointmentsFragment();
            title = "Elder's Appointments";
        } else if (itemId == R.id.nav_alerts) {
            selectedFragment = new AlertsFragment();
            title = "Safety Alerts";
        } else if (itemId == R.id.nav_care_notes) {
            selectedFragment = new CareNotesFragment();
            title = "Care Notes";
        }

        if (selectedFragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .setReorderingAllowed(true)
                    .commit();
            binding.toolbar.setTitle(title);
        }
    }

    private void fetchElderName() {
        db.collection("users").document(elderUid).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    String fName = doc.getString("firstName");
                    String lName = doc.getString("lastName");
                    StringBuilder sb = new StringBuilder();
                    if (fName != null) sb.append(fName);
                    if (lName != null) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(lName);
                    }
                    String fullName = sb.toString().trim();
                    if (!fullName.isEmpty()) {
                        binding.toolbar.setSubtitle("Monitoring: " + fullName);
                    }
                });
    }

    private void observeAlerts() {
        ListenerRegistration alertListener = db.collection("users").document(elderUid).collection("alerts")
                .whereEqualTo("isResolved", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    int count = (value != null) ? value.size() : 0;
                    updateBadge(count);
                    updateToolbarColor(count > 0);
                });
        ListenerRegistry.INSTANCE.register("caregiver_alerts", alertListener);
    }

    private void updateBadge(int count) {
        BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_alerts);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            badge.setVisible(false);
        }
    }

    private void updateToolbarColor(boolean hasAlerts) {
        int targetColor = hasAlerts ? ALERT_RED : PRIMARY_TEAL;
        if (currentPrimaryColor == targetColor) return;

        if (colorAnim != null) colorAnim.cancel();
        colorAnim = ObjectAnimator.ofInt(binding.toolbar, "backgroundColor", currentPrimaryColor, targetColor);
        colorAnim.setDuration(300);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.start();
        currentPrimaryColor = targetColor;
    }

    private void startAlertService() {
        Intent serviceIntent = new Intent(this, CaregiverAlertService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void setupFCM() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) db.collection("users").document(uid).update("fcmToken", token);
            }
        });
        if (elderUid != null) FirebaseMessaging.getInstance().subscribeToTopic("alerts_" + elderUid);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.caregiver_overflow_menu, menu);
        
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.action_logout || item.getItemId() == R.id.action_delete_account) {
                SpannableString s = new SpannableString(item.getTitle());
                s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
                item.setTitle(s);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutConfirmation();
            return true;
        } else if (item.getItemId() == R.id.action_delete_account) {
            showDeleteAccountConfirmation();
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

    private void showDeleteAccountConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Warning: This will permanently delete your account.")
                .setPositiveButton("Delete Forever", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        db.collection("users").document(uid).delete()
                .addOnSuccessListener(aVoid -> {
                    user.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            UserSessionManager.getInstance().logout(this);
                        } else {
                            Toast.makeText(this, "Failed to delete account.", Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Fix 4: Stop rendering operation/animator to prevent EGL disconnect
        if (colorAnim != null) colorAnim.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ListenerRegistry.INSTANCE.unregister("caregiver_alerts");
    }
}
