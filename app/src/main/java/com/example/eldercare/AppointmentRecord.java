package com.example.eldercare;

import com.google.firebase.Timestamp;

public class AppointmentRecord {
    private String id;
    private String doctorName;
    private String specialty;
    private Timestamp dateTime;
    private String location;
    private String doctorPhone;
    private String notes;
    private boolean reminderEnabled;
    private boolean attended;
    private Timestamp markedAttendedAt;

    public AppointmentRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDoctorName() { return doctorName; }
    public String getSpecialty() { return specialty; }
    public Timestamp getDateTime() { return dateTime; }
    public String getLocation() { return location; }
    public String getDoctorPhone() { return doctorPhone; }
    public String getNotes() { return notes; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }
    public Timestamp getMarkedAttendedAt() { return markedAttendedAt; }
    public void setMarkedAttendedAt(Timestamp markedAttendedAt) { this.markedAttendedAt = markedAttendedAt; }
}
