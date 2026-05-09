package com.example.eldercare;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration configuration = newBase.getResources().getConfiguration();
        if (configuration.fontScale < 1.2f) {
            configuration.fontScale = 1.2f;
        }
        Context context = newBase.createConfigurationContext(configuration);
        super.attachBaseContext(context);
    }

    protected void setupBottomNavigation(int selectedItemId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(selectedItemId);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == selectedItemId) return true;

                Intent intent = null;
                if (itemId == R.id.nav_home) {
                    intent = new Intent(this, DashboardActivity.class);
                } else if (itemId == R.id.nav_appointments) {
                    intent = new Intent(this, AppointmentsActivity.class);
                } else if (itemId == R.id.nav_reports) {
                    intent = new Intent(this, ReportsActivity.class);
                } else if (itemId == R.id.nav_meds) {
                    intent = new Intent(this, MedicationActivity.class);
                } else if (itemId == R.id.nav_profile) {
                    intent = new Intent(this, ProfileActivity.class);
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (this instanceof DashboardActivity) {
            new AlertDialog.Builder(this)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
