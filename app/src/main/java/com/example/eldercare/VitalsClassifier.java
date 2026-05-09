package com.example.eldercare;

import android.graphics.Color;

public class VitalsClassifier {

    public enum VitalsStatus {
        NORMAL("Normal", "#2E7D32"),
        ELEVATED("Elevated", "#FBC02D"),
        HIGH("High", "#F57C00"),
        CRITICAL("Critical", "#D32F2F"),
        LOW("Low", "#1976D2");

        public final String label;
        public final String colorHex;

        VitalsStatus(String label, String colorHex) {
            this.label = label;
            this.colorHex = colorHex;
        }

        public int getColor() {
            return Color.parseColor(colorHex);
        }
    }

    public static VitalsStatus classifyBloodPressure(int systolic, int diastolic) {
        if (systolic >= 180 || diastolic >= 120) return VitalsStatus.CRITICAL;
        if (systolic >= 140 || diastolic >= 90) return VitalsStatus.HIGH;
        if (systolic >= 120 || diastolic >= 80) return VitalsStatus.ELEVATED;
        if (systolic < 90 || diastolic < 60) return VitalsStatus.LOW;
        return VitalsStatus.NORMAL;
    }

    public static VitalsStatus classifyGlucose(double mmol) {
        if (mmol >= 11.1) return VitalsStatus.CRITICAL;
        if (mmol >= 7.0) return VitalsStatus.HIGH;
        if (mmol >= 5.6) return VitalsStatus.ELEVATED;
        if (mmol < 3.9) return VitalsStatus.LOW;
        return VitalsStatus.NORMAL;
    }

    public static int getColorForHeartRate(int bpm) {
        if (bpm > 100 || bpm < 50) return Color.parseColor("#C62828"); // Red
        if (bpm > 90 || bpm < 60) return Color.parseColor("#FFB300");  // Amber
        return Color.parseColor("#2E7D32"); // Green
    }

    public static int getColorForBP(int systolic) {
        if (systolic >= 140) return Color.parseColor("#C62828");
        if (systolic >= 120) return Color.parseColor("#FFB300");
        return Color.parseColor("#2E7D32");
    }

    public static int getColorForGlucose(double glucose) {
        if (glucose >= 7.0) return Color.parseColor("#C62828");
        if (glucose >= 5.6) return Color.parseColor("#FFB300");
        return Color.parseColor("#2E7D32");
    }

    public static int getColorForSleep(double hours) {
        if (hours < 4) return Color.parseColor("#C62828"); // Critical
        if (hours < 6 || hours > 10) return Color.parseColor("#FFB300"); // Warning
        return Color.parseColor("#2E7D32"); // Normal
    }
}
