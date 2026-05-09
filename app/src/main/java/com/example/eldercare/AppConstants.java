package com.example.eldercare;

public class AppConstants {
    public static final String FIELD_APP_DOCTOR_NAME = "doctorName";
    public static final String FIELD_APP_SPECIALTY = "specialty";
    public static final String FIELD_APP_DATE_TIME = "dateTime";
    public static final String FIELD_APP_LOCATION = "location";
    public static final String FIELD_APP_NOTES = "notes";
    
    // Additional constants used in the project
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_VITALS = "vitals";
    public static final String COLLECTION_CAREGIVERS = "caregivers";
    public static final String FIELD_RECORDED_AT = "recorded_at";
    public static final String FIELD_STEPS = "steps";
    public static final String FIELD_HEART_RATE = "heart_rate";

    // Notification Groups
    public static final String NOTIFICATION_GROUP_EMERGENCY = "com.example.eldercare.EMERGENCY_GROUP";
    public static final String NOTIFICATION_GROUP_MEDICATION = "com.example.eldercare.MEDICATION_GROUP";
    public static final String NOTIFICATION_GROUP_GENERAL = "com.example.eldercare.GENERAL_GROUP";

    // Notification Channels
    public static final String CHANNEL_EMERGENCY = "emergency_vitals_channel";
    public static final String CHANNEL_MEDICATION = "medication_reminders_v2";
    public static final String CHANNEL_CAREGIVER_MONITOR = "caregiver_monitor_channel";
}
