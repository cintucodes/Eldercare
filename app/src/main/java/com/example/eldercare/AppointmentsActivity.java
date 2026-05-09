package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class AppointmentsActivity extends BaseActivity {

    private RecyclerView rvUpcoming, rvPast;
    private AppointmentAdapter upcomingAdapter, pastAdapter;
    private List<AppointmentRecord> upcomingList, pastList;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration appointmentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        db = FirebaseFirestore.getInstance();
        
        UserSessionManager session = UserSessionManager.getInstance();
        session.init(this);
        if (session.isCaregiver() && session.getLinkedElderId() != null) {
            userId = session.getLinkedElderId();
        } else {
            userId = FirebaseAuth.getInstance().getUid();
        }

        initViews();
        loadAppointments();
        setupBottomNavigation(R.id.nav_appointments);
    }

    private void initViews() {
        rvUpcoming = findViewById(R.id.rvUpcomingAppointments);
        rvPast = findViewById(R.id.rvPastAppointments);

        upcomingList = new ArrayList<>();
        pastList = new ArrayList<>();

        upcomingAdapter = new AppointmentAdapter(upcomingList, this::deleteAppointment);
        pastAdapter = new AppointmentAdapter(pastList, this::deleteAppointment);

        if (rvUpcoming != null) {
            rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
            rvUpcoming.setAdapter(upcomingAdapter);
        }

        if (rvPast != null) {
            rvPast.setLayoutManager(new LinearLayoutManager(this));
            rvPast.setAdapter(pastAdapter);
        }

        View btnPast = findViewById(R.id.btnPastAppointments);
        if (btnPast != null) {
            btnPast.setOnClickListener(v -> {
                if (rvPast != null) {
                    if (rvPast.getVisibility() == View.VISIBLE) {
                        rvPast.setVisibility(View.GONE);
                    } else {
                        rvPast.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        FloatingActionButton fab = findViewById(R.id.fabAddAppointment);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                startActivity(new Intent(this, AddAppointmentActivity.class));
            });
        }
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.title_appointments);
            }
        }
    }

    private void loadAppointments() {
        if (userId == null) return;

        if (appointmentsListener != null) {
            appointmentsListener.remove();
            ListenerRegistry.INSTANCE.unregister("appointments_activity_" + userId);
        }

        appointmentsListener = db.collection("users").document(userId).collection("appointments")
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        upcomingList.clear();
                        pastList.clear();
                        long now = System.currentTimeMillis();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            AppointmentRecord record = doc.toObject(AppointmentRecord.class);
                            if (record != null) {
                                record.setId(doc.getId());
                                if (record.getDateTime() != null && record.getDateTime().toDate().getTime() >= now) {
                                    upcomingList.add(record);
                                } else {
                                    pastList.add(record);
                                }
                            }
                        }
                        upcomingAdapter.notifyDataSetChanged();
                        pastAdapter.notifyDataSetChanged();
                    }
                });
        
        ListenerRegistry.INSTANCE.register("appointments_activity_" + userId, appointmentsListener);
    }

    private void deleteAppointment(AppointmentRecord appointment) {
        if (userId == null || appointment.getId() == null) {
            Toast.makeText(this, "Cannot delete: missing ID", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(userId).collection("appointments")
                .document(appointment.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Appointment deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            if (userId != null) ListenerRegistry.INSTANCE.unregister("appointments_activity_" + userId);
        }
    }
}
