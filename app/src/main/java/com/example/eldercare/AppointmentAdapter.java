package com.example.eldercare;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private final List<AppointmentRecord> appointments;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
    private OnAppointmentDeleteListener deleteListener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnAppointmentDeleteListener {
        void onAppointmentDelete(AppointmentRecord appointment);
    }

    public AppointmentAdapter(List<AppointmentRecord> appointments) {
        this.appointments = appointments;
    }

    public AppointmentAdapter(List<AppointmentRecord> appointments, OnAppointmentDeleteListener deleteListener) {
        this.appointments = appointments;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppointmentRecord appt = appointments.get(position);
        holder.tvDoctor.setText(appt.getDoctorName());
        holder.tvSpecialty.setText(appt.getSpecialty());
        holder.tvLocation.setText(appt.getLocation());
        
        if (appt.getDoctorPhone() != null && !appt.getDoctorPhone().isEmpty()) {
            holder.tvPhoneNumber.setText("📞 " + appt.getDoctorPhone());
            holder.tvPhoneNumber.setVisibility(View.VISIBLE);
            holder.tvPhoneNumber.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + appt.getDoctorPhone()));
                v.getContext().startActivity(intent);
            });
        } else {
            holder.tvPhoneNumber.setVisibility(View.GONE);
        }
        
        if (appt.getDateTime() != null) {
            holder.tvDateTime.setText(dateFormat.format(appt.getDateTime().toDate()));
        }

        UserSessionManager session = UserSessionManager.getInstance();
        boolean isCaregiver = session.isCaregiver();
        
        if (appt.isAttended()) {
            holder.btnMarkAttended.setVisibility(View.GONE);
            holder.chipAttendedStatus.setVisibility(View.VISIBLE);
        } else {
            holder.chipAttendedStatus.setVisibility(View.GONE);
            if (isCaregiver) {
                holder.btnMarkAttended.setVisibility(View.GONE);
            } else {
                holder.btnMarkAttended.setVisibility(View.VISIBLE);
                holder.btnMarkAttended.setOnClickListener(v -> markAsAttended(appt, holder));
            }
        }

        holder.btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + appt.getDoctorPhone()));
            v.getContext().startActivity(intent);
        });

        holder.btnDirections.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(appt.getLocation()));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            v.getContext().startActivity(mapIntent);
        });

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle("Delete Appointment")
                        .setMessage("Are you sure you want to delete this appointment?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (deleteListener != null) {
                                deleteListener.onAppointmentDelete(appt);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void markAsAttended(AppointmentRecord appt, ViewHolder holder) {
        UserSessionManager session = UserSessionManager.getInstance();
        String targetId = session.isCaregiver() ? session.getLinkedElderId() : session.getUid();
        
        if (targetId == null || appt.getId() == null) return;
        
        db.collection("users").document(targetId).collection("appointments")
                .document(appt.getId())
                .update("attended", true, "markedAttendedAt", Timestamp.now())
                .addOnSuccessListener(v -> {
                    appt.setAttended(true);
                    holder.btnMarkAttended.setVisibility(View.GONE);
                    holder.chipAttendedStatus.setVisibility(View.VISIBLE);
                });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctor, tvSpecialty, tvDateTime, tvLocation, tvPhoneNumber;
        MaterialButton btnDirections, btnCall, btnMarkAttended;
        Chip chipAttendedStatus;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctor = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            btnDirections = itemView.findViewById(R.id.btnDirections);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnMarkAttended = itemView.findViewById(R.id.btnMarkAttended);
            chipAttendedStatus = itemView.findViewById(R.id.chipAttendedStatus);
            btnDelete = itemView.findViewById(R.id.btnDeleteAppointment);
        }
    }
}
