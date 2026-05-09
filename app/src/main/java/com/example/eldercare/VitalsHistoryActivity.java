package com.example.eldercare;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class VitalsHistoryActivity extends AppCompatActivity {

    private static final String TAG = "VitalsHistoryActivity";
    private RecyclerView rvHistory;
    private VitalsAdapter adapter;
    private List<VitalsRecord> vitalsList;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration vitalsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitals_history);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        setupToolbar();
        initViews();
        loadVitalsHistory();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Vitals History");
        }
    }

    private void initViews() {
        rvHistory = findViewById(R.id.rvVitalsHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        vitalsList = new ArrayList<>();
        adapter = new VitalsAdapter(vitalsList); 
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void loadVitalsHistory() {
        if (userId == null) return;

        if (vitalsListener != null) {
            vitalsListener.remove();
            ListenerRegistry.INSTANCE.unregister("vitals_history_" + userId);
        }

        progressBar.setVisibility(View.VISIBLE);
        vitalsListener = db.collection("users").document(userId).collection("vitals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Log.e(TAG, "Error loading history", error);
                        return;
                    }

                    if (value != null) {
                        vitalsList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            VitalsRecord record = doc.toObject(VitalsRecord.class);
                            if (record != null) {
                                vitalsList.add(record);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(vitalsList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
        
        ListenerRegistry.INSTANCE.register("vitals_history_" + userId, vitalsListener);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vitalsListener != null) {
            vitalsListener.remove();
            if (userId != null) ListenerRegistry.INSTANCE.unregister("vitals_history_" + userId);
        }
    }
}
