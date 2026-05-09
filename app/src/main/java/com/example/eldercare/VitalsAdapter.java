package com.example.eldercare;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VitalsAdapter extends RecyclerView.Adapter<VitalsAdapter.ViewHolder> {

    private final List<VitalsRecord> vitalsList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault());

    public VitalsAdapter(List<VitalsRecord> vitalsList) {
        this.vitalsList = vitalsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vitals_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VitalsRecord record = vitalsList.get(position);
        
        Timestamp timestamp = record.getRecordedAt();
        if (timestamp != null) {
            holder.tvDate.setText(dateFormat.format(timestamp.toDate()));
        }

        // Heart Rate Chip
        int hr = record.getHeartRate();
        holder.chipHeartRate.setText(hr + " BPM");
        setChipStyle(holder.chipHeartRate, VitalRange.HEART_RATE.getStatus(hr));

        // BP Chip
        double sys = record.getSystolicBP();
        double dia = record.getDiastolicBP();
        if (sys > 0 && dia > 0) {
            holder.chipBP.setVisibility(View.VISIBLE);
            holder.chipBP.setText(String.format(Locale.getDefault(), "%.0f/%.0f", sys, dia));
            
            VitalRange.Status sysStatus = VitalRange.SYSTOLIC.getStatus(sys);
            VitalRange.Status diaStatus = VitalRange.DIASTOLIC.getStatus(dia);
            VitalRange.Status bpStatus = (sysStatus == VitalRange.Status.ABNORMAL || diaStatus == VitalRange.Status.ABNORMAL) 
                    ? VitalRange.Status.ABNORMAL 
                    : (sysStatus == VitalRange.Status.BORDERLINE || diaStatus == VitalRange.Status.BORDERLINE) 
                        ? VitalRange.Status.BORDERLINE 
                        : VitalRange.Status.NORMAL;
            setChipStyle(holder.chipBP, bpStatus);
        } else {
            holder.chipBP.setVisibility(View.GONE);
        }

        // Glucose Chip
        double gluc = record.getBloodGlucose();
        if (gluc > 0) {
            holder.chipGlucose.setVisibility(View.VISIBLE);
            holder.chipGlucose.setText(String.format(Locale.getDefault(), "%.1f mmol/L", gluc));
            setChipStyle(holder.chipGlucose, VitalRange.GLUCOSE.getStatus(gluc));
        } else {
            holder.chipGlucose.setVisibility(View.GONE);
        }

        if (record.getNotes() != null && !record.getNotes().isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("Notes: " + record.getNotes());
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
    }

    private void setChipStyle(Chip chip, VitalRange.Status status) {
        int color;
        switch (status) {
            case NORMAL:
                color = Color.parseColor("#E8F5E9"); // Light Green
                chip.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case BORDERLINE:
                color = Color.parseColor("#FFF3E0"); // Light Orange
                chip.setTextColor(Color.parseColor("#EF6C00"));
                break;
            case ABNORMAL:
            default:
                color = Color.parseColor("#FFEBEE"); // Light Red
                chip.setTextColor(Color.parseColor("#C62828"));
                break;
        }
        chip.setChipBackgroundColor(ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return vitalsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvNotes;
        Chip chipHeartRate, chipBP, chipGlucose;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvNotes = itemView.findViewById(R.id.tvHistoryNotes);
            chipHeartRate = itemView.findViewById(R.id.chipHeartRate);
            chipBP = itemView.findViewById(R.id.chipBP);
            chipGlucose = itemView.findViewById(R.id.chipGlucose);
        }
    }
}
