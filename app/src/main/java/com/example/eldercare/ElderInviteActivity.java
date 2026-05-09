package com.example.eldercare;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ElderInviteActivity extends AppCompatActivity {

    private TextView tvInviteCode, tvExpiryTime;
    private MaterialButton btnGenerateCode, btnCopyCode, btnBack;
    private LinkingCodeManager codeManager;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elder_invite);

        codeManager = new LinkingCodeManager();
        userId = FirebaseAuth.getInstance().getUid();
        
        android.util.Log.d("ElderInviteActivity", "onCreate - userId: " + userId);

        tvInviteCode = findViewById(R.id.tvInviteCode);
        tvExpiryTime = findViewById(R.id.tvExpiryTime);
        btnGenerateCode = findViewById(R.id.btnGenerateCode);
        btnBack = findViewById(R.id.btnBack);
        btnCopyCode = findViewById(R.id.btnCopyCode);
        
        android.util.Log.d("ElderInviteActivity", "Views initialized - btnCopyCode: " + (btnCopyCode != null));

        btnGenerateCode.setOnClickListener(v -> generateCode());
        btnBack.setOnClickListener(v -> finish());
        
        if (btnCopyCode != null) {
            btnCopyCode.setOnClickListener(v -> copyToClipboard());
        }
    }

    private void generateCode() {
        if (userId == null) {
            android.util.Log.e("ElderInviteActivity", "generateCode - userId is null!");
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        android.util.Log.d("ElderInviteActivity", "Generating code for userId: " + userId);
        btnGenerateCode.setEnabled(false);
        
        codeManager.generateCode(userId, new LinkingCodeManager.Callback<String>() {
            @Override
            public void onSuccess(String code) {
                android.util.Log.d("ElderInviteActivity", "Code generated successfully: " + code);
                btnGenerateCode.setEnabled(true);
                tvInviteCode.setText(code);
                tvExpiryTime.setText("Expires in 48 hours");
                Toast.makeText(ElderInviteActivity.this, "Code generated successfully!", Toast.LENGTH_SHORT).show();
                if (btnCopyCode != null) btnCopyCode.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("ElderInviteActivity", "Code generation failed: " + e.getMessage(), e);
                btnGenerateCode.setEnabled(true);
                Toast.makeText(ElderInviteActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void copyToClipboard() {
        String code = tvInviteCode.getText().toString();
        if (code.equals("------")) return;
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Linking Code", code);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
