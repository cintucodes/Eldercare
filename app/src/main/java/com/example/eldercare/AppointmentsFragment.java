package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eldercare.databinding.ActivityAppointmentsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsFragment extends Fragment {

    private ActivityAppointmentsBinding binding;
    private AppointmentAdapter upcomingAdapter;
    private AppointmentAdapter pastAdapter;
    private List<AppointmentRecord> upcomingList;
    private List<AppointmentRecord> pastList;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration appointmentsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityAppointmentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        setupRecyclerViews();
        
        binding.fabAddAppointment.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), AddAppointmentActivity.class));
        });

        binding.btnPastAppointments.setOnClickListener(v -> {
            if (binding.rvPastAppointments.getVisibility() == View.GONE) {
                binding.rvPastAppointments.setVisibility(View.VISIBLE);
                binding.btnPastAppointments.setIconResource(android.R.drawable.arrow_up_float);
            } else {
                binding.rvPastAppointments.setVisibility(View.GONE);
                binding.btnPastAppointments.setIconResource(android.R.drawable.arrow_down_float);
            }
        });

        if (userId != null) {
            fetchAppointments();
        }
    }

    private void setupRecyclerViews() {
        upcomingList = new ArrayList<>();
        pastList = new ArrayList<>();
        
        upcomingAdapter = new AppointmentAdapter(upcomingList, this::deleteAppointment);
        pastAdapter = new AppointmentAdapter(pastList, this::deleteAppointment);

        binding.rvUpcomingAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvUpcomingAppointments.setAdapter(upcomingAdapter);

        binding.rvPastAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPastAppointments.setAdapter(pastAdapter);
    }

    private void fetchAppointments() {
        long now = System.currentTimeMillis();
        
        String targetId = getTargetUserId();

        // Detach previous if exists
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            ListenerRegistry.INSTANCE.unregister("appointments_fragment_" + targetId);
        }

        appointmentsListener = db.collection("users").document(targetId).collection("appointments")
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (binding == null || !isAdded()) return;
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading appointments", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    upcomingList.clear();
                    pastList.clear();

                    for (QueryDocumentSnapshot doc : value) {
                        AppointmentRecord record = doc.toObject(AppointmentRecord.class);
                        record.setId(doc.getId()); // Ensure ID is set for deletion
                        if (record.getDateTime() != null) {
                            long timestamp = record.getDateTime().toDate().getTime();
                            if (timestamp >= now) {
                                upcomingList.add(record);
                            } else {
                                pastList.add(record);
                            }
                        }
                    }
                    upcomingAdapter.notifyDataSetChanged();
                    pastAdapter.notifyDataSetChanged();
                });
        
        // Fix 1: Register in centralized registry to ensure detachment on logout
        ListenerRegistry.INSTANCE.register("appointments_fragment_" + targetId, appointmentsListener);
    }

    private String getTargetUserId() {
        String role = UserSessionManager.getInstance().getRole();
        String linkedElderId = UserSessionManager.getInstance().getLinkedElderId();
        if ("caregiver".equals(role) && linkedElderId != null) {
            return linkedElderId;
        }
        return userId;
    }

    private void deleteAppointment(AppointmentRecord record) {
        if (record.getId() == null) return;
        
        String targetId = getTargetUserId();
        db.collection("users").document(targetId).collection("appointments")
                .document(record.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Appointment deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            ListenerRegistry.INSTANCE.unregister("appointments_fragment_" + getTargetUserId());
        }
        binding = null;
    }
}
