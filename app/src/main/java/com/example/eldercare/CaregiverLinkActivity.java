package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class CaregiverLinkActivity extends AppCompatActivity {

    private TextInputEditText etInviteCode;
    private MaterialButton btnLinkAccount;
    private ProgressBar progressBar;
    private LinkingCodeManager codeManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_link);

        codeManager = new LinkingCodeManager();
        currentUserId = FirebaseAuth.getInstance().getUid();

        etInviteCode = findViewById(R.id.etInviteCode);
        View layoutElderlyEmail = findViewById(R.id.tilElderlyEmail);
        if (layoutElderlyEmail != null) layoutElderlyEmail.setVisibility(View.GONE);

        btnLinkAccount = findViewById(R.id.btnLinkAccount);
        progressBar = findViewById(R.id.progressBar);

        btnLinkAccount.setOnClickListener(v -> validateAndLink());
    }

    private void validateAndLink() {
        String code = etInviteCode.getText().toString().trim();

        if (code.length() != 6) {
            etInviteCode.setError("Enter 6-digit code");
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        codeManager.claimCode(code, currentUserId, new LinkingCodeManager.Callback<String>() {
            @Override
            public void onSuccess(String result) {
                if (isFinishing()) return;
                setLoading(false);
                
                // Bug 2: Topic sync is handled inside setLinkedElderId
                UserSessionManager session = UserSessionManager.getInstance();
                session.init(CaregiverLinkActivity.this);
                session.setLinkedElderId(CaregiverLinkActivity.this, result);

                Toast.makeText(CaregiverLinkActivity.this, "Linked successfully!", Toast.LENGTH_SHORT).show();
                
                Intent intent = new Intent(CaregiverLinkActivity.this, CaregiverMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                if (isFinishing()) return;
                setLoading(false);
                showError("Linking failed: " + e.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLinkAccount.setEnabled(!loading);
        etInviteCode.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
