package com.example.eldercare;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * HealthConnectRationaleActivity explains why the app needs Health Connect permissions.
 * Required by Health Connect guidelines.
 */
public class HealthConnectRationaleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_connect_rationale);

        findViewById(R.id.btnContinue).setOnClickListener(v -> finish());
    }
}
