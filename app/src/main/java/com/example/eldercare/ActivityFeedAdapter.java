package com.example.eldercare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eldercare.databinding.ItemActivityFeedBinding;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityFeedAdapter extends RecyclerView.Adapter<ActivityFeedAdapter.ViewHolder> {

    private List<Map<String, Object>> activities;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public ActivityFeedAdapter(List<Map<String, Object>> activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemActivityFeedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> activity = activities.get(position);
        String type = (String) activity.get("type");
        Timestamp timestamp = (Timestamp) activity.get("timestamp");

        // Reset visibility in case it was hidden before
        holder.itemView.setVisibility(View.VISIBLE);
        holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        ));

        String displayValue = "";
        int iconRes = R.drawable.baseline_monitor_heart_24;
        int color = 0;

        if (type == null) {
            // Hide items without type
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }

        switch (type) {
            case "heartRate":
            case "heart_rate":
                int hr = activity.get("value") != null ? ((Number) activity.get("value")).intValue() : 0;
                displayValue = "Heart Rate: " + hr + " bpm";
                iconRes = R.drawable.baseline_monitor_heart_24;
                color = VitalsClassifier.getColorForHeartRate(hr);
                break;

            case "blood_pressure":
                int sys = activity.get("systolic") != null ? ((Number) activity.get("systolic")).intValue() : 0;
                int dia = activity.get("diastolic") != null ? ((Number) activity.get("diastolic")).intValue() : 0;
                displayValue = "Blood Pressure: " + sys + "/" + dia + " mmHg";
                iconRes = R.drawable.baseline_bloodtype_24;
                color = VitalsClassifier.getColorForBP(sys);
                break;

            case "bloodGlucose":
            case "glucose":
                double glucose = activity.get("value") != null ? ((Number) activity.get("value")).doubleValue() : 0.0;
                displayValue = String.format(Locale.getDefault(), "Glucose: %.1f mmol/L", glucose);
                iconRes = R.drawable.baseline_opacity_24;
                color = VitalsClassifier.getColorForGlucose(glucose);
                break;

            case "steps":
                int steps = activity.get("value") != null ? ((Number) activity.get("value")).intValue() : 0;
                displayValue = "Steps: " + steps;
                iconRes = R.drawable.baseline_directions_walk_24;
                color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark);
                break;

            case "sleepHours":
                double sleep = activity.get("value") != null ? ((Number) activity.get("value")).doubleValue() : 0.0;
                displayValue = String.format(Locale.getDefault(), "Sleep: %.1f hrs", sleep);
                iconRes = R.drawable.baseline_bedtime_24;
                color = VitalsClassifier.getColorForSleep(sleep);
                break;

            case "vitals_entry":
                displayValue = "Complete Vitals Set Logged";
                iconRes = R.drawable.baseline_bar_chart_24;
                color = holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray);
                break;

            default:
                // Skip unknown types - don't display them
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                return;
        }

        holder.binding.ivActivityIcon.setImageResource(iconRes);
        holder.binding.tvActivityValue.setText(displayValue);
        holder.binding.tvActivityTime.setText(timestamp != null ? dateFormat.format(timestamp.toDate()) : "");

        if (color != 0) {
            holder.binding.vStatusDot.setBackgroundColor(color);
        } else {
            holder.binding.vStatusDot.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));
        }
    }

    @Override
    public int getItemCount() {
        return activities != null ? activities.size() : 0;
    }

    public void updateData(List<Map<String, Object>> newActivities) {
        this.activities = newActivities;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemActivityFeedBinding binding;
        public ViewHolder(ItemActivityFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
