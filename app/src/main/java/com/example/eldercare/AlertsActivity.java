package com.example.eldercare;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class AlertsActivity extends AppCompatActivity {

    private static final String TAG = "AlertsActivity";
    private RecyclerView rvAlerts;
    private AlertAdapter adapter;
    private List<AlertRecord> alertList;
    private List<AlertRecord> filteredList;
    private LinearLayout emptyState;
    private FirestoreHelper firestoreHelper;
    private String currentFilter = "All";
    private ListenerRegistration alertsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        firestoreHelper = new FirestoreHelper();
        setupToolbar();
        initViews();
        loadAlerts();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        rvAlerts = findViewById(R.id.rvAlerts);
        emptyState = findViewById(R.id.emptyState);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        alertList = new ArrayList<>();
        filteredList = new ArrayList<>();
        
        adapter = new AlertAdapter(new AlertAdapter.OnAlertActionListener() {
            @Override
            public void onResolve(AlertRecord alert) {
                dismissAlert(alert);
            }

            @Override
            public void onAcknowledge(AlertRecord alert) {
                acknowledgeAlert(alert);
            }

            @Override
            public void onDelete(AlertRecord alert) {
                // Delete not implemented in this activity
            }
        });

        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() != null) {
                    currentFilter = tab.getText().toString();
                    applyFilter();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadAlerts() {
        String userId = firestoreHelper.getCurrentUserId();
        if (userId == null) return;

        if (alertsListener != null) {
            alertsListener.remove();
            ListenerRegistry.INSTANCE.unregister("alerts_activity_" + userId);
        }

        alertsListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("alerts")
                .whereEqualTo("isResolved", false) // Consistency: Show active alerts
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for alerts", error);
                        return;
                    }
                    if (value == null) return;
                    
                    alertList.clear();
                    for (DocumentSnapshot doc : value) {
                        AlertRecord alert = doc.toObject(AlertRecord.class);
                        if (alert != null) {
                            alert.setId(doc.getId());
                            alertList.add(alert);
                        }
                    }
                    applyFilter();
                });
        
        ListenerRegistry.INSTANCE.register("alerts_activity_" + userId, alertsListener);
    }

    private void applyFilter() {
        filteredList.clear();
        for (AlertRecord alert : alertList) {
            if (currentFilter.equals("All") ||
                (currentFilter.equals("Critical") && "CRITICAL".equals(alert.getType())) ||
                (currentFilter.equals("Warnings") && "WARNING".equals(alert.getType())) ||
                (currentFilter.equals("Info") && "INFO".equals(alert.getType()))) {
                filteredList.add(alert);
            }
        }
        
        adapter.submitList(new ArrayList<>(filteredList));
        emptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        rvAlerts.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void acknowledgeAlert(AlertRecord alert) {
        String userId = firestoreHelper.getCurrentUserId();
        if (userId == null || alert.getId() == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("alerts")
                .document(alert.getId())
                .update("isAcknowledged", true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Alert acknowledged", Toast.LENGTH_SHORT).show());
    }

    private void dismissAlert(AlertRecord alert) {
        String userId = firestoreHelper.getCurrentUserId();
        if (userId == null || alert.getId() == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("alerts")
                .document(alert.getId())
                .update("isResolved", true) // Using isResolved for consistency with modern flow
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Alert resolved", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertsListener != null) {
            alertsListener.remove();
            String userId = firestoreHelper.getCurrentUserId();
            if (userId != null) ListenerRegistry.INSTANCE.unregister("alerts_activity_" + userId);
        }
    }
}
