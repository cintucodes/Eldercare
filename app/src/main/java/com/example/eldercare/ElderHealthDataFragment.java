package com.example.eldercare;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eldercare.databinding.FragmentElderHealthDataBinding;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ElderHealthDataFragment extends Fragment {

    private static final String TAG = "ElderHealthDataFrag";
    private FragmentElderHealthDataBinding binding;
    private FirebaseFirestore db;
    private String elderUid;
    private String currentType = "heartRate"; 
    private int daysRange = 7;
    
    private final Map<String, ListenerRegistration> listeners = new HashMap<>();

    private static class HealthPoint {
        long time;
        float value;
        HealthPoint(long time, float value) {
            this.time = time;
            this.value = value;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentElderHealthDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) {
            showNoElderState();
            return;
        }

        db.collection("users").document(myUid).get().addOnSuccessListener(doc -> {
            if (!isAdded() || binding == null) return;
            
            if (!doc.exists()) {
                showNoElderState();
                return;
            }

            String role = doc.getString("role");
            if ("elder".equals(role)) {
                elderUid = myUid;
            } else if ("caregiver".equals(role)) {
                elderUid = doc.getString("linkedElderId");
            }

            if (elderUid == null) {
                showNoElderState();
                return;
            }

            setupChart();
            setupListeners();
            setupStepGoalSection();
            startObservingData(currentType);
        }).addOnFailureListener(e -> {
            if (isAdded()) showNoElderState();
        });
    }

    private void showNoElderState() {
        if (binding != null) {
            binding.loadingIndicator.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyTitle.setText("No Elder Data Available");
            binding.healthChart.setVisibility(View.GONE);
        }
    }

    private void setupChart() {
        if (binding == null) return;
        binding.healthChart.getDescription().setEnabled(false);
        binding.healthChart.setDrawGridBackground(false);
        binding.healthChart.setTouchEnabled(true);
        binding.healthChart.setDragEnabled(true);
        binding.healthChart.setScaleEnabled(true);
        binding.healthChart.setPinchZoom(true);

        XAxis xAxis = binding.healthChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(new Date((long) value));
            }
        });

        binding.healthChart.getAxisLeft().setDrawGridLines(true);
        binding.healthChart.getAxisRight().setEnabled(false);

        Legend l = binding.healthChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setEnabled(true);
    }

    private void setupListeners() {
        if (binding == null) return;
        
        binding.btnUpdateStepGoal.setOnClickListener(v -> updateStepGoal());
        
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentType = "heartRate"; break;
                    case 1: currentType = "systolicBP"; break;
                    case 2: currentType = "bloodGlucose"; break;
                    case 3: currentType = "steps"; break;
                    case 4: currentType = "sleepHours"; break;
                }
                startObservingData(currentType);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.cgTimeRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip7D)) daysRange = 7;
            else if (checkedIds.contains(R.id.chip30D)) daysRange = 30;
            else if (checkedIds.contains(R.id.chip90D)) daysRange = 90;
            startObservingData(currentType);
        });
    }

    private void setupStepGoalSection() {
        if (binding == null || elderUid == null) return;
        
        db.collection("users").document(elderUid).get()
            .addOnSuccessListener(doc -> {
                if (binding != null && doc.exists()) {
                    Long stepGoal = doc.getLong("stepGoal");
                    binding.etStepGoal.setText(String.valueOf(stepGoal != null ? stepGoal : 5000));
                }
            });
    }

    private void updateStepGoal() {
        if (binding == null || elderUid == null) return;
        
        String stepGoalStr = binding.etStepGoal.getText().toString().trim();
        if (stepGoalStr.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "Please enter a step goal", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int stepGoal = Integer.parseInt(stepGoalStr);
            if (stepGoal < 1000) stepGoal = 1000;
            if (stepGoal > 50000) stepGoal = 50000;
            
            int finalStepGoal = stepGoal;
            db.collection("users").document(elderUid)
                .update("stepGoal", stepGoal)
                .addOnSuccessListener(v -> {
                    if (isAdded()) {
                        android.widget.Toast.makeText(getContext(), "Step goal updated to " + finalStepGoal, android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        android.widget.Toast.makeText(getContext(), "Failed to update step goal", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        } catch (NumberFormatException e) {
            android.widget.Toast.makeText(getContext(), "Please enter a valid number", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void startObservingData(String type) {
        if (binding == null || elderUid == null) return;
        
        String listenerKey = "elder_health_data_" + type;
        if (listeners.containsKey(type)) {
            listeners.get(type).remove();
            ListenerRegistry.INSTANCE.unregister(listenerKey);
        }

        binding.loadingIndicator.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysRange);
        Date startDate = cal.getTime();

        ListenerRegistration reg = db.collection("users").document(elderUid).collection("vitals")
                .whereEqualTo("type", type)
                .addSnapshotListener((value, error) -> {
                    if (binding == null || !isAdded()) return;
                    binding.loadingIndicator.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e(TAG, "Listen failed for " + type, error);
                        showEmptyState();
                        return;
                    }

                    if (value != null) {
                        List<HealthPoint> points = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Timestamp ts = doc.getTimestamp("timestamp");
                            Object valObj = doc.get("value");
                            
                            if (ts != null && valObj instanceof Number) {
                                Date recordDate = ts.toDate();
                                if (recordDate.after(startDate)) {
                                    points.add(new HealthPoint(recordDate.getTime(), ((Number) valObj).floatValue()));
                                }
                            }
                        }

                        Collections.sort(points, (a, b) -> Long.compare(a.time, b.time));

                        if (points.isEmpty()) {
                            showEmptyState();
                        } else if (type.equals(currentType)) { // Ensure we only update chart for active tab
                            List<Entry> entries = new ArrayList<>();
                            for (HealthPoint p : points) entries.add(new Entry(p.time, p.value));
                            displayChart(entries);
                        }
                    }
                });
        
        listeners.put(type, reg);
        ListenerRegistry.INSTANCE.register(listenerKey, reg);
    }

    private void showEmptyState() {
        if (binding != null) {
            binding.healthChart.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyTitle.setText("No " + currentType + " data for this period");
        }
    }

    private void displayChart(List<Entry> entries) {
        if (binding != null) {
            binding.emptyState.setVisibility(View.GONE);
            binding.healthChart.setVisibility(View.VISIBLE);
            String label = currentType;

            LineDataSet dataSet = new LineDataSet(entries, label);
            dataSet.setColor(Color.parseColor("#00695C"));
            dataSet.setCircleColor(Color.parseColor("#00695C"));
            dataSet.setLineWidth(2f);
            dataSet.setDrawValues(false);

            binding.healthChart.setData(new LineData(dataSet));
            binding.healthChart.invalidate();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (String type : listeners.keySet()) {
            ListenerRegistration reg = listeners.get(type);
            if (reg != null) reg.remove();
            ListenerRegistry.INSTANCE.unregister("elder_health_data_" + type);
        }
        listeners.clear();
        binding = null;
    }
}
