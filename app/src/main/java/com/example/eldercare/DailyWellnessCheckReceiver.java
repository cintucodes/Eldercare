package com.example.eldercare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class DailyWellnessCheckReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "daily_wellness_check";
    private static final String GROUP_KEY_WELLNESS = "com.example.eldercare.WELLNESS_GROUP";
    private static final int SUMMARY_ID = 6001;
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Daily Wellness Check", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);

        // Individual Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Good morning! 💊")
                .setContentText("Time for your daily health check-in. Have you taken your vitals yet?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(GROUP_KEY_WELLNESS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Summary Notification
        Notification summaryNotification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Wellness Checks")
                .setContentText("Daily health reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle("Wellness Checks")
                        .setSummaryText("Daily Routine"))
                .setGroup(GROUP_KEY_WELLNESS)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NOTIFICATION_ID, builder.build());
        notificationManager.notify(SUMMARY_ID, summaryNotification);
    }
}
