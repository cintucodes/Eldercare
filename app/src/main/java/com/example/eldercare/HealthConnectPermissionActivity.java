package com.example.eldercare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import java.util.Set;

public class HealthConnectPermissionActivity extends AppCompatActivity {

    private HealthConnectManager healthConnectManager;
    private ActivityResultLauncher<Set<String>> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hc_permission);

        healthConnectManager = new HealthConnectManager(this);

        requestPermissionLauncher = healthConnectManager.requestPermissionLauncher(this, granted -> {
            if (granted) {
                savePermissionState(true);
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Permissions are required for sync", Toast.LENGTH_SHORT).show();
            }
        });

        MaterialButton btnConnect = findViewById(R.id.btnConnectHC);
        MaterialButton btnSkip = findViewById(R.id.btnSkipHC);

        btnConnect.setOnClickListener(v -> healthConnectManager.requestPermissions(requestPermissionLauncher));
        btnSkip.setOnClickListener(v -> {
            savePermissionState(false);
            finish();
        });
    }

    private void savePermissionState(boolean granted) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("hc_permissions_granted", granted).apply();
    }
}
