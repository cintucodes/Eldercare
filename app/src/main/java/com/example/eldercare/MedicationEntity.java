package com.example.eldercare;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medications")
public class MedicationEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String userId;
    public String name;
    public String dosage;
    public String time;
    public boolean syncedToFirestore = false;

    public MedicationEntity(String userId, String name, String dosage, String time) {
        this.userId = userId;
        this.name = name;
        this.dosage = dosage;
        this.time = time;
    }
}
