package com.example.eldercare;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ACTIVE = 0;
    private static final int TYPE_PAST = 1;

    public interface OnMedicationTakenListener {
        void onMedicationTaken(Medication medication, boolean isChecked);
    }

    public interface OnMedicationDeleteListener {
        void onMedicationDelete(Medication medication);
    }

    private List<Medication> medications;
    private final OnMedicationTakenListener takenListener;
    private final OnMedicationDeleteListener deleteListener;
    private final boolean isPastMode;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Active list constructor
    public MedicationAdapter(List<Medication> medications, OnMedicationTakenListener listener) {
        this.medications = medications;
        this.takenListener = listener;
        this.deleteListener = null;
        this.isPastMode = false;
    }

    // Past list constructor
    public MedicationAdapter(List<Medication> medications, OnMedicationDeleteListener listener) {
        this.medications = medications;
        this.takenListener = null;
        this.deleteListener = listener;
        this.isPastMode = true;
    }

    // Management/view-only constructor
    public MedicationAdapter(List<Medication> medications) {
        this.medications = medications;
        this.takenListener = null;
        this.deleteListener = null;
        this.isPastMode = false;
    }

    // Delete-mode constructor (management screen)
    public MedicationAdapter(List<Medication> medications, boolean isDeleteMode, OnMedicationDeleteListener listener) {
        this.medications = medications;
        this.takenListener = null;
        this.deleteListener = listener;
        this.isPastMode = isDeleteMode;
    }

    @Override
    public int getItemViewType(int position) {
        return isPastMode ? TYPE_PAST : TYPE_ACTIVE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_PAST) {
            return new PastViewHolder(inflater.inflate(R.layout.item_medication, parent, false));
        }
        return new ActiveViewHolder(inflater.inflate(R.layout.item_medication_small, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Medication med = medications.get(position);
        if (med == null) return;

        if (holder instanceof ActiveViewHolder) {
            bindActive((ActiveViewHolder) holder, med);
        } else {
            bindPast((PastViewHolder) holder, med);
        }
    }

    private void bindActive(ActiveViewHolder h, Medication med) {
        h.tvName.setText(med.getName());
        h.tvDosage.setText(med.getDosage());
        h.tvTime.setText(med.getTime());

        UserSessionManager session = UserSessionManager.getInstance();
        boolean isCaregiver = session.isCaregiver();

        if (isCaregiver) {
            h.btnMarkTaken.setVisibility(View.GONE);
        } else {
            h.btnMarkTaken.setVisibility(View.VISIBLE);
            applyTakenState(h, med.isTakenToday());

            h.btnMarkTaken.setOnClickListener(v -> {
                if (!med.isTakenToday()) {
                    med.setTakenToday(true);
                    applyTakenState(h, true);
                    updateMedicationAdherence(med, true);
                    if (takenListener != null) takenListener.onMedicationTaken(med, true);
                }
            });
        }
    }

    private void applyTakenState(ActiveViewHolder h, boolean taken) {
        if (taken) {
            h.btnMarkTaken.setText("Taken ✓");
            h.btnMarkTaken.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            h.btnMarkTaken.setEnabled(false);
        } else {
            h.btnMarkTaken.setText("Mark as Taken");
            h.btnMarkTaken.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D52")));
            h.btnMarkTaken.setEnabled(true);
        }
    }

    private void bindPast(PastViewHolder h, Medication med) {
        h.tvName.setText(med.getName());
        h.tvDosageTime.setText(med.getDosage() + (med.getTime() != null ? " · " + med.getTime() : ""));

        Timestamp takenAt = med.getTaken_at();
        if (takenAt != null) {
            String formatted = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(takenAt.toDate());
            h.tvTakenAt.setText("Taken at " + formatted);
        } else {
            h.tvTakenAt.setText("");
        }

        h.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onMedicationDelete(med);
        });
    }

    private void updateMedicationAdherence(Medication med, boolean taken) {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null || med.getId() == null) return;

        UserSessionManager session = UserSessionManager.getInstance();
        String targetId = (session.isCaregiver() && session.getLinkedElderId() != null)
                ? session.getLinkedElderId() : myUid;

        Timestamp now = Timestamp.now();
        db.collection("users").document(targetId).collection("medications")
                .document(med.getId()).update("takenToday", true, "taken_at", now);

        Map<String, Object> log = new HashMap<>();
        log.put("medicationId", med.getId());
        log.put("medicationName", med.getName());
        log.put("dosage", med.getDosage());
        log.put("status", "Taken");
        log.put("timestamp", now);
        db.collection("users").document(targetId).collection("medication_logs").add(log);
    }

    @Override
    public int getItemCount() {
        return medications != null ? medications.size() : 0;
    }

    public void updateData(List<Medication> newMedications) {
        this.medications = newMedications;
        notifyDataSetChanged();
    }

    static class ActiveViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDosage, tvTime;
        MaterialButton btnMarkTaken;

        ActiveViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMedName);
            tvDosage = itemView.findViewById(R.id.tvMedDosage);
            tvTime = itemView.findViewById(R.id.tvMedTime);
            btnMarkTaken = itemView.findViewById(R.id.btnMarkTaken);
        }
    }

    static class PastViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDosageTime, tvTakenAt;
        ImageButton btnDelete;

        PastViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMedNameItem);
            tvDosageTime = itemView.findViewById(R.id.tvDosageTimeItem);
            tvTakenAt = itemView.findViewById(R.id.tvTakenAtItem);
            btnDelete = itemView.findViewById(R.id.btnDeleteMed);
        }
    }
}
