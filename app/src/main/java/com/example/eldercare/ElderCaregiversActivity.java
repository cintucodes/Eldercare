package com.example.eldercare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ElderCaregiversActivity extends AppCompatActivity {

    private static final String TAG = "ElderCaregiversActivity";
    
    private LinearLayout llCaregiversList;
    private LinearLayout llEmptyState;
    private MaterialButton btnClose;
    private MaterialButton btnAddCaregiver;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elder_caregivers);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        llCaregiversList = findViewById(R.id.llCaregiversList);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnClose = findViewById(R.id.btnClose);
        btnAddCaregiver = findViewById(R.id.btnAddCaregiver);

        btnClose.setOnClickListener(v -> finish());
        btnAddCaregiver.setOnClickListener(v -> {
            startActivity(new Intent(this, ElderInviteActivity.class));
        });

        loadCaregivers();
    }

    private void loadCaregivers() {
        if (uid == null) {
            Toast.makeText(this, "Error: Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading caregivers for elder: " + uid);

        db.collection("users").document(uid).collection("caregivers")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        Log.d(TAG, "No caregivers found");
                        showEmptyState();
                        return;
                    }

                    Log.d(TAG, "Found " + snapshots.size() + " caregivers");
                    llEmptyState.setVisibility(View.GONE);
                    llCaregiversList.setVisibility(View.VISIBLE);
                    llCaregiversList.removeAllViews();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String email = doc.getString("email");
                        String relationship = doc.getString("relationship");

                        if (name == null) name = "Unknown";
                        if (relationship == null) relationship = "Caregiver";

                        addCaregiverCard(name, phone, email, relationship);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load caregivers: " + e.getMessage());
                    Toast.makeText(this, "Failed to load caregivers", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void addCaregiverCard(String name, String phone, String email, String relationship) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_elder_caregiver_card, llCaregiversList, false);

        TextView tvName = cardView.findViewById(R.id.tvCaregiverName);
        TextView tvPhone = cardView.findViewById(R.id.tvPhone);
        TextView tvEmail = cardView.findViewById(R.id.tvEmail);
        TextView tvRelationship = cardView.findViewById(R.id.tvRelationship);
        LinearLayout llEmail = cardView.findViewById(R.id.llEmail);
        MaterialButton btnCall = cardView.findViewById(R.id.btnCall);

        tvName.setText(name);
        tvRelationship.setText(relationship);

        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
            btnCall.setOnClickListener(v -> makeCall(phone));
        } else {
            tvPhone.setText("No phone number");
            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);
        }

        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
            llEmail.setVisibility(View.VISIBLE);
        } else {
            llEmail.setVisibility(View.GONE);
        }

        llCaregiversList.addView(cardView);
    }

    private void makeCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    private void showEmptyState() {
        llCaregiversList.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }
}
