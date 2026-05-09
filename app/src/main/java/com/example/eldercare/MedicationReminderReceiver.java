package com.example.eldercare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

/**
 * BroadcastReceiver for handling medication reminders with Sound and High Importance.
 * Reschedules the next alarm for daily frequency.
 */
public class MedicationReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "MedReminderReceiver";
    public static final String CHANNEL_ID = "medication_reminders_v2";
    private static final String GROUP_KEY_MEDICATION = "com.example.eldercare.MEDICATION_GROUP";
    private static final int SUMMARY_ID = 4001;

    @Override
    public void onReceive(Context context, Intent intent) {
        String medId = intent.getStringExtra("medId");
        String medName = intent.getStringExtra("medName");
        String dosage = intent.getStringExtra("dosage");
        String time = intent.getStringExtra("time");
        int index = intent.getIntExtra("reminderIndex", 0);

        Log.d(TAG, "Reminder received for: " + medName);

        showNotification(context, medId, medName, dosage);
        enqueueMissedMedicationCheck(context, medId, medName);
        
        // Reschedule for the next day since setExactAndAllowWhileIdle is one-shot
        if (time != null) {
            MedicationReminderScheduler.scheduleAlarm(context, medId, medName, dosage, time, index);
        }
    }

    private void showNotification(Context context, String medId, String name, String dosage) {
        NotificationManager notificationManager = (NotificationManager) 
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Emergency Medication Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Critical reminders for your health.");
            channel.enableLights(true);
            channel.enableVibration(true);
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(alarmSound, audioAttributes);
            
            notificationManager.createNotificationChannel(channel);
        }

        Intent reminderIntent = new Intent(context, MedicationReminderActivity.class);
        reminderIntent.putExtra("medId", medId);
        reminderIntent.putExtra("medName", name);
        reminderIntent.putExtra("dosage", dosage);
        reminderIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (medId != null ? medId.hashCode() : 0), 
                reminderIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action: Mark as Taken (quick action)
        Intent takenIntent = new Intent(context, MedicationReminderActivity.class);
        takenIntent.putExtra("medId", medId);
        takenIntent.putExtra("medName", name);
        takenIntent.putExtra("quick_action", "mark_taken");
        takenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent takenPendingIntent = PendingIntent.getActivity(context, 
                (medId != null ? medId.hashCode() + 100 : 100), takenIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action: Snooze (10 minutes)
        Intent snoozeIntent = new Intent(context, MedicationReminderActivity.class);
        snoozeIntent.putExtra("medId", medId);
        snoozeIntent.putExtra("medName", name);
        snoozeIntent.putExtra("quick_action", "snooze");
        snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent snoozePendingIntent = PendingIntent.getActivity(context, 
                (medId != null ? medId.hashCode() + 200 : 200), snoozeIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Individual Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_medication_24)
                .setContentTitle("💊 Medication Time")
                .setContentText("Please take " + name + " (" + dosage + ")")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Time to take your medication:\n" + name + " - " + dosage + "\n\nTap to confirm or snooze."))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_MEDICATION)
                .setFullScreenIntent(pendingIntent, true) // Wake up the screen
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.baseline_check_circle_24, "Taken", takenPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePendingIntent);

        // Summary Notification
        Notification summaryNotification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Medication Reminders")
                .setContentText("You have medication reminders")
                .setSmallIcon(R.drawable.baseline_medication_24)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle("Medication Reminders")
                        .setSummaryText("Health Schedule"))
                .setGroup(GROUP_KEY_MEDICATION)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .build();

        int notificationId = (medId != null ? medId.hashCode() : (int) System.currentTimeMillis());
        notificationManager.notify(notificationId, builder.build());
        notificationManager.notify(SUMMARY_ID, summaryNotification);
    }

    private void enqueueMissedMedicationCheck(Context context, String medId, String medName) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null || medId == null) return;

        Data inputData = new Data.Builder()
                .putString("medId", medId)
                .putString("userId", userId)
                .putString("medName", medName)
                .build();

        OneTimeWorkRequest missedCheckRequest = new OneTimeWorkRequest.Builder(MissedMedicationWorker.class)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .addTag("missed_" + medId)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueue(missedCheckRequest);
    }
}
