package com.example.eldercare;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eldercare.databinding.ItemAlertBinding;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AlertAdapter extends ListAdapter<AlertRecord, AlertAdapter.ViewHolder> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    private final OnAlertActionListener listener;

    public interface OnAlertActionListener {
        void onResolve(AlertRecord alert);
        void onAcknowledge(AlertRecord alert);
        void onDelete(AlertRecord alert);
    }

    public AlertAdapter(OnAlertActionListener listener) {
        super(new DiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAlertBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertRecord alert = getItem(position);

        holder.binding.tvAlertMessage.setText(alert.getMessage() != null ? alert.getMessage() : alert.getType());
        holder.binding.tvAlertTime.setText(alert.getTimestamp() != null ? dateFormat.format(alert.getTimestamp().toDate()) : "");

        if ("SOS".equals(alert.getType()) || "health_sos".equals(alert.getType())) {
            holder.binding.ivAlertIcon.setImageResource(R.drawable.baseline_emergency_share_24);
        } else {
            holder.binding.ivAlertIcon.setImageResource(R.drawable.baseline_warning_24);
        }

        // Fix 3: UI for Acknowledged vs Resolved
        if (alert.isResolved()) {
            holder.binding.chipResolved.setVisibility(View.VISIBLE);
            holder.binding.chipResolved.setText("Resolved");
            holder.binding.btnMarkResolved.setVisibility(View.GONE);
            holder.binding.btnAcknowledge.setVisibility(View.GONE);
        } else if (alert.isAcknowledged()) {
            holder.binding.chipResolved.setVisibility(View.VISIBLE);
            holder.binding.chipResolved.setText("Acknowledged");
            holder.binding.btnMarkResolved.setVisibility(View.VISIBLE);
            holder.binding.btnAcknowledge.setVisibility(View.GONE);
        } else {
            holder.binding.chipResolved.setVisibility(View.GONE);
            holder.binding.btnMarkResolved.setVisibility(View.VISIBLE);
            holder.binding.btnAcknowledge.setVisibility(View.VISIBLE);
        }

        Double lat = alert.getGpsLat();
        Double lng = alert.getGpsLng();

        if (lat != null && lng != null) {
            holder.binding.btnViewLocation.setVisibility(View.VISIBLE);
            holder.binding.btnViewLocation.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                v.getContext().startActivity(mapIntent);
            });
        } else {
            holder.binding.btnViewLocation.setVisibility(View.GONE);
        }

        holder.binding.btnMarkResolved.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResolve(alert);
            }
        });

        holder.binding.btnAcknowledge.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcknowledge(alert);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDelete(alert);
            }
            return true;
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemAlertBinding binding;
        public ViewHolder(ItemAlertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<AlertRecord> {
        @Override
        public boolean areItemsTheSame(@NonNull AlertRecord oldItem, @NonNull AlertRecord newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AlertRecord oldItem, @NonNull AlertRecord newItem) {
            boolean sameStatus = oldItem.isResolved() == newItem.isResolved() && 
                   oldItem.isAcknowledged() == newItem.isAcknowledged();
            boolean sameMessage = (oldItem.getMessage() != null && oldItem.getMessage().equals(newItem.getMessage()));
            boolean sameLocation = (oldItem.getGpsLat() != null && oldItem.getGpsLat().equals(newItem.getGpsLat())) &&
                                   (oldItem.getGpsLng() != null && oldItem.getGpsLng().equals(newItem.getGpsLng()));
            return sameStatus && sameMessage && sameLocation;
        }
    }
}
