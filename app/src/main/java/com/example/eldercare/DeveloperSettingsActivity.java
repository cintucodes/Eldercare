package com.example.eldercare;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class DeveloperSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Safety check - though UI should be hidden
        if (!com.example.eldercare.BuildConfig.DEBUG) {
            finish();
            return;
        }

        setContentView(R.layout.activity_developer_settings);

        String elderUid = FirebaseAuth.getInstance().getUid();

        MaterialButton btnSeed = findViewById(R.id.btnSeedData);
        MaterialButton btnClear = findViewById(R.id.btnClearData);

        btnSeed.setOnClickListener(v -> {
            if (elderUid == null) return;
            
            btnSeed.setEnabled(false);
            MockDataSeeder.seedDemoData(elderUid)
                    .addOnSuccessListener(aVoid -> {
                        btnSeed.setEnabled(true);
                        Snackbar.make(v, "Demo data seeded successfully!", Snackbar.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSeed.setEnabled(true);
                        Toast.makeText(this, "Seeding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnClear.setOnClickListener(v -> {
            if (elderUid == null) return;

            btnClear.setEnabled(false);
            MockDataSeeder.clearDemoData(elderUid)
                    .addOnSuccessListener(aVoid -> {
                        btnClear.setEnabled(true);
                        Snackbar.make(v, "All demo data cleared.", Snackbar.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        btnClear.setEnabled(true);
                        Toast.makeText(this, "Clear failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
