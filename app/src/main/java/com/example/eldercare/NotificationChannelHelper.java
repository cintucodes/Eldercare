package com.example.eldercare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Helper class to manage notification channels.
 */
public class NotificationChannelHelper {

    public static final String CHANNEL_VITALS = "vitals_alerts";
    public static final String CHANNEL_MEDICATIONS = "medication_alerts";
    public static final String CHANNEL_SOS = "sos_alerts";

    /**
     * Creates all notification channels required by the app.
     * Should be called early in the app lifecycle (e.g., Application.onCreate or first activity).
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Vitals Channel
            NotificationChannel vitalsChannel = new NotificationChannel(
                    CHANNEL_VITALS,
                    "Health Vitals Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            vitalsChannel.setDescription("Notifications for vital sign abnormalities");

            // Medications Channel
            NotificationChannel medicationsChannel = new NotificationChannel(
                    CHANNEL_MEDICATIONS,
                    "Medication Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            medicationsChannel.setDescription("Reminders to take scheduled medications");

            // SOS Channel
            NotificationChannel sosChannel = new NotificationChannel(
                    CHANNEL_SOS,
                    "SOS Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            sosChannel.setDescription("Urgent emergency and SOS notifications");

            manager.createNotificationChannel(vitalsChannel);
            manager.createNotificationChannel(medicationsChannel);
            manager.createNotificationChannel(sosChannel);
        }
    }
}
