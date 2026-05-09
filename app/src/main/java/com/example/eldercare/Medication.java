package com.example.eldercare;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.List;

/**
 * Model class for Medication data.
 */
public class Medication {
    private String id;
    private String name;
    private String dosage;
    private String frequency;
    private String time;
    private boolean takenToday;
    private boolean isActive = true;
    private String status; // "pending", "taken", "missed"
    private Timestamp taken_at;
    private Object timestamp;

    // Missing fields identified in logs
    private String instructions;
    private List<String> times;
    private String doctorName;
    private String type;
    private Object startDate; // Changed to Object to handle Firestore Timestamps
    private boolean remindersEnabled;

    // Required empty constructor for Firestore
    public Medication() {}

    public Medication(String id, String name, String dosage, String frequency, String time) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.time = time;
        this.takenToday = false;
        this.isActive = true;
        this.status = "pending";
        this.timestamp = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public boolean isTakenToday() { return takenToday; }
    public void setTakenToday(boolean takenToday) { this.takenToday = takenToday; }

    @PropertyName("isActive")
    public boolean isActive() { return isActive; }

    @PropertyName("isActive")
    public void setActive(boolean active) { isActive = active; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getTaken_at() { return taken_at; }
    public void setTaken_at(Timestamp taken_at) { this.taken_at = taken_at; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public List<String> getTimes() { return times; }
    public void setTimes(List<String> times) { this.times = times; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Object getStartDate() { return startDate; }
    public void setStartDate(Object startDate) { this.startDate = startDate; }

    public boolean isRemindersEnabled() { return remindersEnabled; }
    public void setRemindersEnabled(boolean remindersEnabled) { this.remindersEnabled = remindersEnabled; }

    @PropertyName("timestamp")
    public Object getTimestampRaw() { return timestamp; }

    @PropertyName("timestamp")
    public void setTimestampRaw(Object timestamp) { this.timestamp = timestamp; }

    @Exclude
    public Timestamp getTimestamp() {
        if (timestamp instanceof Timestamp) {
            return (Timestamp) timestamp;
        } else if (timestamp instanceof Long) {
            return new Timestamp(new Date((Long) timestamp));
        }
        return null;
    }
}
