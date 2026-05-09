package com.example.eldercare;

public enum VitalRange {
    HEART_RATE(60, 100, 40, 150),
    SYSTOLIC(90, 130, 70, 190),
    DIASTOLIC(60, 85, 40, 110),
    GLUCOSE(3.9, 7.8, 2.0, 20.0),
    TEMPERATURE(36.1, 37.2, 34.0, 42.0);

    public final double minNormal;
    public final double maxNormal;
    public final double minWarning;
    public final double maxWarning;

    VitalRange(double minNormal, double maxNormal, double minWarning, double maxWarning) {
        this.minNormal = minNormal;
        this.maxNormal = maxNormal;
        this.minWarning = minWarning;
        this.maxWarning = maxWarning;
    }

    public Status getStatus(double value) {
        if (value < minWarning || value > maxWarning) return Status.ABNORMAL;
        if (value < minNormal || value > maxNormal) return Status.BORDERLINE;
        return Status.NORMAL;
    }

    public enum Status {
        NORMAL, BORDERLINE, ABNORMAL
    }
}
