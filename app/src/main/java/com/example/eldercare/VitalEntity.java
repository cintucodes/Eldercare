package com.example.eldercare;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vitals")
public class VitalEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String userId;
    public double heartRate;
    public String bloodPressure;
    public double bloodGlucose;
    public long timestamp;
    public boolean syncedToFirestore = false;

    public VitalEntity(String userId, double heartRate, String bloodPressure, double bloodGlucose, long timestamp) {
        this.userId = userId;
        this.heartRate = heartRate;
        this.bloodPressure = bloodPressure;
        this.bloodGlucose = bloodGlucose;
        this.timestamp = timestamp;
    }
}
