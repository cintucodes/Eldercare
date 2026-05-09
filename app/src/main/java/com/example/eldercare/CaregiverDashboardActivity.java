package com.example.eldercare;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaregiverDashboardActivity extends AppCompatActivity implements PatientAdapter.OnPatientActionListener {

    private static final String TAG = "CaregiverDashboard";
    private RecyclerView rvPatients, rvCareNotes;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddNote;
    
    private PatientAdapter patientAdapter;
    private CareNoteAdapter noteAdapter;
    private List<PatientRecord> patientList;
    private List<CareNoteAdapter.CareNote> noteList;
    
    private FirebaseFirestore db;
    private String caregiverUid;
    private String linkedElderlyEmail;
    private String linkedElderlyId;
    private ListenerRegistration notesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_dashboard);

        db = FirebaseFirestore.getInstance();
        caregiverUid = FirebaseAuth.getInstance().getUid();
        
        initViews();
        loadCaregiverProfile();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rvPatients = findViewById(R.id.rvPatients);
        rvCareNotes = findViewById(R.id.rvCareNotes);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        fabAddNote = findViewById(R.id.fabAddNote);

        patientList = new ArrayList<>();
        patientAdapter = new PatientAdapter(patientList, this);
        rvPatients.setLayoutManager(new LinearLayoutManager(this));
        rvPatients.setAdapter(patientAdapter);

        noteList = new ArrayList<>();
        noteAdapter = new CareNoteAdapter(noteList);
        rvCareNotes.setLayoutManager(new LinearLayoutManager(this));
        rvCareNotes.setAdapter(noteAdapter);

        fabAddNote.setOnClickListener(v -> showAddNoteDialog());
    }

    private void loadCaregiverProfile() {
        if (caregiverUid == null) return;
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("users").document(caregiverUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fName = doc.getString("firstName");
                        String lName = doc.getString("lastName");
                        String caregiverName = (fName != null ? fName : "");

                        if (caregiverName != null && !caregiverName.isEmpty()) {
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle("Hello, " + caregiverName);
                            }
                        }
                        
                        linkedElderlyId = doc.getString("linkedElderId");
                        if (linkedElderlyId != null) {
                            fetchElderlyDetailsById(linkedElderlyId);
                        } else {
                            linkedElderlyEmail = doc.getString("linkedElderlyEmail");
                            if (linkedElderlyEmail != null) {
                                fetchElderlyDetailsByEmail(linkedElderlyEmail);
                            } else {
                                showEmptyState("No elderly user linked.");
                            }
                        }
                    }
                });
    }

    private void fetchElderlyDetailsById(String elderId) {
        db.collection("users").document(elderId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        processElderlyDocument(doc);
                    } else {
                        showEmptyState("Linked user not found.");
                    }
                });
    }

    private void fetchElderlyDetailsByEmail(String email) {
        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "elder")
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        processElderlyDocument(query.getDocuments().get(0));
                    } else {
                        showEmptyState("Elderly user not found.");
                    }
                });
    }

    private void processElderlyDocument(DocumentSnapshot doc) {
        linkedElderlyId = doc.getId();
        
        PatientRecord patient = new PatientRecord();
        patient.setId(doc.getId());
        
        String fName = doc.getString("firstName");
        String lName = doc.getString("lastName");
        StringBuilder sb = new StringBuilder();
        if (fName != null) sb.append(fName);
        if (lName != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lName);
        }
        String fullName = sb.toString().trim();
        if (fullName.isEmpty()) fullName = doc.getString("name");
        if (fullName == null) fullName = "Elderly User";
        
        patient.setFullName(fullName);
        
        String ageStr = doc.getString("age");
        if (ageStr != null) {
            try {
                patient.setAge(Integer.parseInt(ageStr));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing age: " + ageStr);
                patient.setAge(0);
            }
        }
        
        patient.setPhone(doc.getString("phone"));
        
        db.collection("users").document(linkedElderlyId).collection("vitals")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
                .get().addOnSuccessListener(vitals -> {
                    if (!vitals.isEmpty()) {
                        patient.setStatus("NORMAL");
                    }
                    patientList.clear();
                    patientList.add(patient);
                    patientAdapter.notifyDataSetChanged();
                    hideEmptyState();
                    loadCareNotes(linkedElderlyId);
                });
    }

    private void loadCareNotes(String elderId) {
        if (notesListener != null) {
            notesListener.remove();
            ListenerRegistry.INSTANCE.unregister("caregiver_dashboard_notes_" + elderId);
        }

        notesListener = db.collection("users").document(elderId).collection("careNotes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        noteList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            CareNoteAdapter.CareNote note = doc.toObject(CareNoteAdapter.CareNote.class);
                            if (note != null) noteList.add(note);
                        }
                        noteAdapter.notifyDataSetChanged();
                    }
                });
        
        ListenerRegistry.INSTANCE.register("caregiver_dashboard_notes_" + elderId, notesListener);
    }

    private void showAddNoteDialog() {
        if (linkedElderlyId == null) return;

        final EditText etNote = new EditText(this);
        etNote.setHint("Enter care note...");

        new AlertDialog.Builder(this)
                .setTitle("Add Care Note")
                .setView(etNote)
                .setPositiveButton("Add", (dialog, which) -> {
                    String noteText = etNote.getText().toString();
                    if (!noteText.isEmpty()) {
                        saveCareNote(noteText);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveCareNote(String text) {
        String caregiverName = UserSessionManager.getInstance().getFullName();
        if (caregiverName.isEmpty()) caregiverName = "Caregiver";

        Map<String, Object> note = new HashMap<>();
        note.put("text", text);
        note.put("authorUid", caregiverUid);
        note.put("caregiverName", caregiverName); 
        note.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(linkedElderlyId).collection("careNotes")
                .add(note)
                .addOnSuccessListener(ref -> Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAcknowledgeAlert(String patientId) {
        db.collection("users").document(patientId).collection("alerts")
                .whereEqualTo("isAcknowledged", false)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isAcknowledged", true);
                        updates.put("acknowledgedBy", caregiverUid);
                        updates.put("acknowledgedAt", FieldValue.serverTimestamp());
                        doc.getReference().update(updates);
                    }
                    Toast.makeText(this, "Alerts acknowledged", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEmptyState(String msg) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (tvEmptyState != null) {
            tvEmptyState.setText(msg);
            tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyState() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notesListener != null) {
            notesListener.remove();
            if (linkedElderlyId != null) {
                ListenerRegistry.INSTANCE.unregister("caregiver_dashboard_notes_" + linkedElderlyId);
            }
        }
    }
}
