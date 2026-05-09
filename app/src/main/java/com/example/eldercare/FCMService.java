package com.example.eldercare;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMService extends FirebaseMessagingService {

    private static final String GROUP_KEY_EMERGENCY = "com.example.eldercare.EMERGENCY_GROUP";
    private static final String GROUP_KEY_MEDICATION = "com.example.eldercare.MEDICATION_GROUP";
    private static final String GROUP_KEY_ALERTS = "com.example.eldercare.GENERAL_ALERTS_GROUP";
    
    private static final int EMERGENCY_SUMMARY_ID = 2001;
    private static final int MEDICATION_SUMMARY_ID = 4001;
    private static final int ALERTS_SUMMARY_ID = 5001;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        String type = remoteMessage.getData().get("type");
        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "Health Alert";
        String body = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "";

        showNotification(title, body, type);
    }

    private void showNotification(String title, String body, String type) {
        String channelId = NotificationChannelHelper.CHANNEL_VITALS;
        String groupKey = GROUP_KEY_ALERTS;
        int summaryId = ALERTS_SUMMARY_ID;
        String summaryTitle = "Health Alerts";
        int icon = android.R.drawable.ic_dialog_alert;

        if ("MEDICATION".equals(type)) {
            channelId = NotificationChannelHelper.CHANNEL_MEDICATIONS;
            groupKey = GROUP_KEY_MEDICATION;
            summaryId = MEDICATION_SUMMARY_ID;
            summaryTitle = "Medication Reminders";
        } else if ("SOS".equals(type)) {
            channelId = NotificationChannelHelper.CHANNEL_SOS;
            groupKey = GROUP_KEY_EMERGENCY;
            summaryId = EMERGENCY_SUMMARY_ID;
            summaryTitle = "Emergency Alerts";
        }

        Intent intent = new Intent(this, AlertsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);

        // Individual Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(body)
                .setGroup(groupKey)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Fix 3: Group Summary - Silent to prevent system muting
        Notification summaryNotification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(summaryTitle)
                .setContentText("You have new health notifications")
                .setSmallIcon(icon)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle(summaryTitle)
                        .setSummaryText("ElderCare Health"))
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setSilent(true)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            notificationManager.notify(summaryId, summaryNotification);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("fcmToken", token);
        }
    }
}
