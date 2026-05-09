package com.example.eldercare;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_sos_alerts")
public class PendingSosAlert {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public double latitude;
    public double longitude;
    public long timestamp;

    public PendingSosAlert(double latitude, double longitude, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
}
