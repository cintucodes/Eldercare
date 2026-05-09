package com.example.eldercare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Worker that reminds elders to sync health data if they haven't synced in 24 hours
 */
public class HealthSyncReminderWorker extends Worker {
    
    private static final String CHANNEL_ID = "health_sync_reminder";
    private static final int NOTIFICATION_ID = 9001;
    
    public HealthSyncReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return Result.success();
        
        // Check if user is elder and hasn't synced in 24 hours
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && "elder".equals(doc.getString("role"))) {
                    Long lastSync = doc.getLong("lastHealthConnectSync");
                    long now = System.currentTimeMillis();
                    
                    // If never synced or synced more than 24 hours ago
                    if (lastSync == null || (now - lastSync) > 24 * 60 * 60 * 1000) {
                        showReminderNotification();
                    }
                }
            });
        
        return Result.success();
    }
    
    private void showReminderNotification() {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Health Sync Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Reminders to sync health data from wearable devices");
            manager.createNotificationChannel(channel);
        }
        
        // Intent to open profile tab
        Intent intent = new Intent(context, ElderMainActivity.class);
        intent.putExtra("openProfileTab", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to Sync Health Data")
            .setContentText("Haven't synced in 24 hours. Tap to sync from your wearable.")
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("You haven't synced health data in 24 hours. Keep your caregivers updated by syncing from Health Connect."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
