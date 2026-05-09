package com.example.eldercare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AppointmentReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "appointment_reminders";
    private static final String CHANNEL_NAME = "Appointment Reminders";
    private static final String GROUP_KEY_APPOINTMENTS = "com.example.eldercare.APPOINTMENTS_GROUP";
    private static final int SUMMARY_ID = 3001;

    @Override
    public void onReceive(Context context, Intent intent) {
        String doctor = intent.getStringExtra("doctor");
        String location = intent.getStringExtra("location");
        String appId = intent.getStringExtra("appId");

        if (doctor == null) doctor = "Doctor";
        if (location == null) location = "the clinic";

        createNotificationChannel(context);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
            context, 
            0, 
            new Intent(context, ElderMainActivity.class).putExtra("openAppointments", true),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_calendar_today_24)
                .setContentTitle("Upcoming Appointment")
                .setContentText("Appointment with " + doctor + " at " + location + " in 1 hour")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Appointment with " + doctor + " at " + location + " in 1 hour\n\nTap to view all appointments"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_KEY_APPOINTMENTS)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        Notification summaryNotification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Appointments")
                .setContentText("You have upcoming appointments")
                .setSmallIcon(R.drawable.baseline_calendar_today_24)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle("Upcoming Appointments")
                        .setSummaryText("Reminders"))
                .setGroup(GROUP_KEY_APPOINTMENTS)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = (appId != null) ? appId.hashCode() : (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
            notificationManager.notify(SUMMARY_ID, summaryNotification);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
