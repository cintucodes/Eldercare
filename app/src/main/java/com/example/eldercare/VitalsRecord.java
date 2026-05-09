package com.example.eldercare;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class VitalsRecord {
    private int heartRate;
    private double systolicBP;
    private double diastolicBP;
    private double bloodGlucose;
    private double temperature;
    private double weight;
    private int steps;
    private double sleepHours;
    private String notes;
    private Timestamp timestamp;

    public VitalsRecord() {} // Required for Firestore

    public int getHeartRate() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }

    public double getSystolicBP() { return systolicBP; }
    public void setSystolicBP(double systolicBP) { this.systolicBP = systolicBP; }

    public double getDiastolicBP() { return diastolicBP; }
    public void setDiastolicBP(double diastolicBP) { this.diastolicBP = diastolicBP; }

    public double getBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(double bloodGlucose) { this.bloodGlucose = bloodGlucose; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    public double getSleepHours() { return sleepHours; }
    public void setSleepHours(double sleepHours) { this.sleepHours = sleepHours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @PropertyName("recordedAt")
    public Timestamp getRecordedAt() { return timestamp; }
    @PropertyName("recordedAt")
    public void setRecordedAt(Timestamp recordedAt) { this.timestamp = recordedAt; }
}
