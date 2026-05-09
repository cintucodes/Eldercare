package com.example.eldercare;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "health_snapshots")
public class HealthSnapshot {
    @PrimaryKey
    @NonNull
    public String uid;
    public long timestamp;
    public double heartRate;
    public long steps;
    public int bloodPressureSystolic;
    public int bloodPressureDiastolic;

    public HealthSnapshot(@NonNull String uid, long timestamp, double heartRate, long steps, int bloodPressureSystolic, int bloodPressureDiastolic) {
        this.uid = uid;
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.steps = steps;
        this.bloodPressureSystolic = bloodPressureSystolic;
        this.bloodPressureDiastolic = bloodPressureDiastolic;
    }
}
