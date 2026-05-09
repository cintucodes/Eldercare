package com.example.eldercare;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etPhone, etAge, etNewAllergy;
    private MaterialButton btnSave, btnAddAllergy;
    private ChipGroup cgAllergies;
    private FirebaseFirestore db;
    private String userId;
    private List<String> allergyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        setupToolbar();
        initViews();
        loadCurrentProfile();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        etNewAllergy = findViewById(R.id.etNewAllergy);
        btnAddAllergy = findViewById(R.id.btnAddAllergy);
        cgAllergies = findViewById(R.id.cgEditAllergies);
        btnSave = findViewById(R.id.btnSaveProfile);

        btnAddAllergy.setOnClickListener(v -> addAllergyFromInput());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void addAllergyFromInput() {
        String allergy = etNewAllergy.getText().toString().trim();
        if (!TextUtils.isEmpty(allergy)) {
            addAllergyChip(allergy);
            etNewAllergy.setText("");
        }
    }

    private void addAllergyChip(String allergy) {
        if (allergyList.contains(allergy)) return;
        
        allergyList.add(allergy);
        Chip chip = new Chip(this);
        chip.setText(allergy);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            cgAllergies.removeView(chip);
            allergyList.remove(allergy);
        });
        cgAllergies.addView(chip);
    }

    private void loadCurrentProfile() {
        if (userId == null) return;

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etFirstName.setText(documentSnapshot.getString("firstName"));
                etLastName.setText(documentSnapshot.getString("lastName"));
                etPhone.setText(documentSnapshot.getString("phone"));
                etAge.setText(documentSnapshot.getString("age"));
                
                List<String> existingAllergies = (List<String>) documentSnapshot.get("allergies");
                if (existingAllergies != null) {
                    for (String allergy : existingAllergies) {
                        addAllergyChip(allergy);
                    }
                }
            }
        });
    }

    private void saveProfile() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String age = etAge.getText().toString().trim();

        if (TextUtils.isEmpty(fName) || TextUtils.isEmpty(lName)) {
            Toast.makeText(this, "Name fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", fName);
        updates.put("lastName", lName);
        updates.put("phone", phone);
        updates.put("age", age);
        updates.put("allergies", allergyList);

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local session to reflect new name
                    UserSessionManager.getInstance().updateName(this, fName, lName);
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
