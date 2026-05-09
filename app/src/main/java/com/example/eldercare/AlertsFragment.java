package com.example.eldercare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eldercare.databinding.FragmentAlertsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    private FragmentAlertsBinding binding;
    private FirebaseFirestore db;
    private String elderUid;
    private AlertAdapter adapter;
    private ListenerRegistration alertsListener;
    private List<AlertRecord> allAlerts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        elderUid = UserSessionManager.getInstance().getLinkedElderId();

        if (elderUid == null) {
            binding.loadingIndicator.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
            return;
        }

        setupRecyclerView();
        setupClearButton();
        observeAlerts();
    }

    private void setupRecyclerView() {
        adapter = new AlertAdapter(new AlertAdapter.OnAlertActionListener() {
            @Override
            public void onResolve(AlertRecord alert) {
                resolveAlert(alert);
            }

            @Override
            public void onAcknowledge(AlertRecord alert) {
                acknowledgeAlert(alert);
            }

            @Override
            public void onDelete(AlertRecord alert) {
                confirmDeleteAlert(alert);
            }
        });
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAlerts.setAdapter(adapter);
    }

    private void setupClearButton() {
        binding.btnClearResolved.setOnClickListener(v -> confirmClearResolved());
    }

    private void updateClearButtonVisibility() {
        boolean hasResolved = false;
        for (AlertRecord alert : allAlerts) {
            if (alert.isResolved()) {
                hasResolved = true;
                break;
            }
        }
        binding.btnClearResolved.setVisibility(hasResolved ? View.VISIBLE : View.GONE);
    }

    private void confirmClearResolved() {
        int resolvedCount = 0;
        for (AlertRecord alert : allAlerts) {
            if (alert.isResolved()) resolvedCount++;
        }

        if (resolvedCount == 0) {
            Toast.makeText(getContext(), "No resolved alerts to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Resolved Alerts")
                .setMessage("Delete all " + resolvedCount + " resolved alerts?")
                .setPositiveButton("Clear", (dialog, which) -> clearResolvedAlerts())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearResolvedAlerts() {
        if (elderUid == null) return;

        int deletedCount = 0;
        for (AlertRecord alert : allAlerts) {
            if (alert.isResolved() && alert.getId() != null) {
                db.collection("users").document(elderUid)
                        .collection("alerts").document(alert.getId())
                        .delete();
                deletedCount++;
            }
        }

        if (deletedCount > 0) {
            Toast.makeText(getContext(), deletedCount + " resolved alerts cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeAlerts() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        
        if (alertsListener != null) {
            alertsListener.remove();
            ListenerRegistry.INSTANCE.unregister("alerts_fragment_" + elderUid);
        }

        alertsListener = db.collection("users").document(elderUid).collection("alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (binding == null || !isAdded()) return;
                    binding.loadingIndicator.setVisibility(View.GONE);
                    if (error != null) {
                        Snackbar.make(binding.getRoot(), "Failed to load alerts.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.btnClearResolved.setVisibility(View.GONE);
                        adapter.submitList(new ArrayList<>());
                        allAlerts.clear();
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        allAlerts.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            AlertRecord alert = doc.toObject(AlertRecord.class);
                            alert.setId(doc.getId());
                            allAlerts.add(alert);
                        }
                        adapter.submitList(new ArrayList<>(allAlerts));
                        updateClearButtonVisibility();
                    }
                });
        
        ListenerRegistry.INSTANCE.register("alerts_fragment_" + elderUid, alertsListener);
    }

    private void acknowledgeAlert(AlertRecord alert) {
        if (elderUid == null || alert.getId() == null) return;

        db.collection("users").document(elderUid)
                .collection("alerts").document(alert.getId())
                .update("isAcknowledged", true)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Alert acknowledged", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), "Failed to acknowledge alert.", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void resolveAlert(AlertRecord alert) {
        if (elderUid == null || alert.getId() == null) return;
        
        db.collection("users").document(elderUid)
                .collection("alerts").document(alert.getId())
                .update("isResolved", true)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Alert marked as resolved", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), "Failed to update alert.", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void confirmDeleteAlert(AlertRecord alert) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Alert")
                .setMessage("Are you sure you want to permanently delete this alert record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAlert(alert))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAlert(AlertRecord alert) {
        if (elderUid == null || alert.getId() == null) return;

        db.collection("users").document(elderUid)
                .collection("alerts").document(alert.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Alert deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), "Failed to delete alert.", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertsListener != null) {
            alertsListener.remove();
            ListenerRegistry.INSTANCE.unregister("alerts_fragment_" + elderUid);
        }
        binding = null;
    }
}
