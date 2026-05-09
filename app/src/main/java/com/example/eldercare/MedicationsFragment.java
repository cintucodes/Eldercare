package com.example.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eldercare.databinding.FragmentMedicationsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MedicationsFragment extends Fragment {

    private FragmentMedicationsBinding binding;
    private FirebaseFirestore db;
    private String targetId;
    private MedicationAdapter activeAdapter;
    private MedicationAdapter pastAdapter;
    private boolean isPastExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMedicationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        UserSessionManager session = UserSessionManager.getInstance();
        session.init(requireContext());
        targetId = session.isCaregiver() ? session.getLinkedElderId() : session.getUid();

        setupRecyclerViews();

        binding.fabAddMedication.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddMedicationActivity.class)));

        if (session.isCaregiver()) {
            binding.fabAddMedication.setVisibility(View.GONE);
        }

        binding.layoutPastHeader.setOnClickListener(v -> togglePastSection());

        startListeningForMedications();
    }

    private void setupRecyclerViews() {
        binding.rvMedications.setLayoutManager(new LinearLayoutManager(getContext()));
        
        UserSessionManager session = UserSessionManager.getInstance();
        boolean isCaregiver = session.isCaregiver();
        
        if (isCaregiver) {
            activeAdapter = new MedicationAdapter(new ArrayList<>());
        } else {
            activeAdapter = new MedicationAdapter(new ArrayList<>(), (medication, isChecked) -> {});
        }
        binding.rvMedications.setAdapter(activeAdapter);

        binding.rvPastMedications.setLayoutManager(new LinearLayoutManager(getContext()));
        pastAdapter = new MedicationAdapter(new ArrayList<>(), this::showDeleteLogConfirmation);
        binding.rvPastMedications.setAdapter(pastAdapter);
    }

    private void togglePastSection() {
        isPastExpanded = !isPastExpanded;
        binding.rvPastMedications.setVisibility(isPastExpanded ? View.VISIBLE : View.GONE);
        // Rotate chevron to indicate state
        binding.ivPastChevron.setRotation(isPastExpanded ? 180f : 0f);
    }

    private void startListeningForMedications() {
        if (targetId == null) return;

        binding.loadingIndicator.setVisibility(View.VISIBLE);

        ListenerRegistration medsListener = db.collection("users").document(targetId)
                .collection("medications")
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || binding == null) return;
                    binding.loadingIndicator.setVisibility(View.GONE);

                    if (error != null) {
                        Snackbar.make(binding.getRoot(), "Error loading medications", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    if (value != null) {
                        List<Medication> activeMeds = new ArrayList<>();
                        List<Medication> pastMeds = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : value) {
                            Medication med = doc.toObject(Medication.class);
                            if (med != null) {
                                med.setId(doc.getId());
                                if (med.isActive()) {
                                    activeMeds.add(med);  // Active medications
                                } else {
                                    pastMeds.add(med);  // Discontinued medications
                                }
                            }
                        }
                        updateUI(activeMeds, pastMeds);
                    }
                });

        ListenerRegistry.INSTANCE.register("medications_listener", medsListener);
    }

    private void updateUI(List<Medication> activeMeds, List<Medication> pastMeds) {
        if (activeMeds.isEmpty() && pastMeds.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.tvActiveHeader.setVisibility(View.GONE);
            binding.layoutPastHeader.setVisibility(View.GONE);
            binding.rvMedications.setVisibility(View.GONE);
            binding.rvPastMedications.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);

            binding.tvActiveHeader.setVisibility(activeMeds.isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvMedications.setVisibility(activeMeds.isEmpty() ? View.GONE : View.VISIBLE);
            if (!activeMeds.isEmpty()) activeAdapter.updateData(activeMeds);

            if (!pastMeds.isEmpty()) {
                binding.layoutPastHeader.setVisibility(View.VISIBLE);
                binding.tvPastCount.setText(String.valueOf(pastMeds.size()));
                pastAdapter.updateData(pastMeds);
                // Keep collapsed state; only show list if already expanded
                binding.rvPastMedications.setVisibility(isPastExpanded ? View.VISIBLE : View.GONE);
            } else {
                binding.layoutPastHeader.setVisibility(View.GONE);
                binding.rvPastMedications.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Permanently deletes the medication from the database.
     */
    private void showDeleteLogConfirmation(Medication med) {
        UserSessionManager session = UserSessionManager.getInstance();
        if (session.isCaregiver()) {
            return;
        }
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Medication?")
                .setMessage("Are you sure you want to permanently delete " + med.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMedication(med))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMedication(Medication med) {
        if (targetId == null || med.getId() == null) return;
        // Permanently delete the medication document
        db.collection("users").document(targetId).collection("medications")
                .document(med.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && binding != null)
                        Snackbar.make(binding.getRoot(), "Medication deleted", Snackbar.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ListenerRegistry.INSTANCE.unregister("medications_listener");
        binding = null;
    }
}
