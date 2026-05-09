package com.example.eldercare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;

public class AppointmentReminderWorker extends Worker {
    private static final String TAG = "ApptReminderWorker";
    private static final String CHANNEL_ID = "appointment_reminders";
    private static final String GROUP_KEY_APPOINTMENTS = "com.example.eldercare.APPOINTMENTS_GROUP";
    private static final int SUMMARY_ID = 3001;

    public AppointmentReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String appointmentId = getInputData().getString("appointmentId");
        String doctorName = getInputData().getString("doctorName");
        String time = getInputData().getString("time");
        String reminderType = getInputData().getString("reminderType"); // "1h" or "24h"

        if (appointmentId == null) return Result.failure();

        sendNotification(doctorName, time, reminderType, appointmentId);
        updateFirestore(appointmentId, reminderType);

        return Result.success();
    }

    private void sendNotification(String doctor, String time, String type, String appId) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Appointment Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        String message = "Reminder: Appointment with " + doctor + " at " + time;
        if ("24h".equals(type)) message = "Tomorrow: " + message;

        Intent intent = new Intent(getApplicationContext(), AppointmentsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), appId.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Individual Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_calendar_today_24)
                .setContentTitle("Medical Appointment")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP_KEY_APPOINTMENTS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Summary Notification
        Notification summaryNotification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Appointments")
                .setContentText("Upcoming medical appointments")
                .setSmallIcon(R.drawable.baseline_calendar_today_24)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle("Medical Appointments")
                        .setSummaryText("Reminders"))
                .setGroup(GROUP_KEY_APPOINTMENTS)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(appId.hashCode(), builder.build());
        notificationManager.notify(SUMMARY_ID, summaryNotification);
    }

    private void updateFirestore(String id, String type) {
        String field = "24h".equals(type) ? "reminder_24h_sent" : "reminder_1h_sent";
        FirebaseFirestore.getInstance().collection("appointments").document(id).update(field, true)
                .addOnFailureListener(e -> Log.e(TAG, "Update failed", e));
    }
}
