package com.example.eldercare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class SOSActivity extends AppCompatActivity {

    private static final String TAG = "SOSActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvCountdown;
    private MaterialButton btnSOS, btnCancel;
    private String userId;
    private CountDownTimer countDownTimer;
    private boolean isCountingDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        userId = FirebaseAuth.getInstance().getUid();

        tvCountdown = findViewById(R.id.tvSOSCountdown);
        btnSOS = findViewById(R.id.btnSOSAction);
        btnCancel = findViewById(R.id.btnSOSCancel);

        btnSOS.setOnClickListener(v -> startSOSFlow());
        btnCancel.setOnClickListener(v -> cancelSOS());
    }

    private void startSOSFlow() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }
        
        // Check if location services are enabled
        SOSManager sosManager = SOSManager.getInstance(this);
        if (!sosManager.isLocationEnabled()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Location Services Disabled")
                    .setMessage("Please enable location services (GPS or Network) to send accurate SOS alerts with your location.")
                    .setPositiveButton("Enable", (dialog, which) -> {
                        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Send Anyway", (dialog, which) -> proceedWithCountdown())
                    .setNeutralButton("Cancel", null)
                    .show();
            return;
        }
        
        proceedWithCountdown();
    }
    
    private void proceedWithCountdown() {
        btnSOS.setVisibility(View.GONE);
        btnCancel.setVisibility(View.VISIBLE);
        tvCountdown.setVisibility(View.VISIBLE);
        isCountingDown = true;

        countDownTimer = new CountDownTimer(4000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                if (secondsRemaining > 0) {
                    tvCountdown.setText(String.valueOf(secondsRemaining));
                }
            }
            @Override
            public void onFinish() {
                isCountingDown = false;
                triggerSOS();
            }
        }.start();
    }

    private void cancelSOS() {
        if (countDownTimer != null) countDownTimer.cancel();
        isCountingDown = false;
        tvCountdown.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnSOS.setVisibility(View.VISIBLE);
        Toast.makeText(this, "SOS Alert Cancelled", Toast.LENGTH_SHORT).show();
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ requires background location as separate permission
            ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, 
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void triggerSOS() {
        tvCountdown.setText("Sending...");
        btnCancel.setVisibility(View.GONE);

        if (userId != null) {
            // Use the consolidated SOSManager to prevent double alerts and improve location
            SOSManager.getInstance(this).triggerSOS(userId);
            Toast.makeText(this, "SOS Alert Sent!", Toast.LENGTH_LONG).show();
            
            // Finish activity after a small delay to show the "Sending..." status
            new android.os.Handler().postDelayed(this::finish, 1500);
        } else {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}
