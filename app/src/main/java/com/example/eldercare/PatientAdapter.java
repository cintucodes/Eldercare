package com.example.eldercare;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {

    private final List<PatientRecord> patients;
    private final OnPatientActionListener actionListener;

    public interface OnPatientActionListener {
        void onAcknowledgeAlert(String patientId);
    }

    public PatientAdapter(List<PatientRecord> patients, OnPatientActionListener actionListener) {
        this.patients = patients;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PatientRecord patient = patients.get(position);
        holder.tvName.setText(patient.getFullName());
        holder.tvStatus.setText("Status: " + patient.getStatus());

        int statusColor;
        boolean isCritical = "CRITICAL".equalsIgnoreCase(patient.getStatus()) || "WARNING".equalsIgnoreCase(patient.getStatus());
        
        if ("CRITICAL".equalsIgnoreCase(patient.getStatus())) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.alert_red);
            holder.btnAckAlert.setVisibility(View.VISIBLE);
        } else if ("WARNING".equalsIgnoreCase(patient.getStatus())) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.alert_amber);
            holder.btnAckAlert.setVisibility(View.VISIBLE);
        } else {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green);
            holder.btnAckAlert.setVisibility(View.GONE);
        }
        holder.viewStatusIndicator.setBackgroundColor(statusColor);

        holder.btnContactElder.setOnClickListener(v -> {
            if (patient.getPhone() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + patient.getPhone()));
                holder.itemView.getContext().startActivity(intent);
            }
        });

        holder.btnAckAlert.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAcknowledgeAlert(patient.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivPhoto;
        TextView tvName, tvStatus;
        View viewStatusIndicator;
        MaterialButton btnContactElder, btnAckAlert;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPatientPhoto);
            tvName = itemView.findViewById(R.id.tvPatientName);
            tvStatus = itemView.findViewById(R.id.tvPatientStatus);
            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            btnContactElder = itemView.findViewById(R.id.btnContactElder);
            btnAckAlert = itemView.findViewById(R.id.btnAckAlert);
        }
    }
}
