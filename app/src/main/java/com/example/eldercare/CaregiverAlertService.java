package com.example.eldercare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CaregiverAlertService extends Service {

    private static final String TAG = "CaregiverAlertService";
    private static final String CHANNEL_ID = "caregiver_monitor_channel";
    private static final String EMERGENCY_CHANNEL_ID = "emergency_vitals_channel";
    private static final String GROUP_KEY_ALERTS = "com.example.eldercare.eldercare_alerts";
    private static final int SERVICE_NOTIFICATION_ID = 1001;
    private static final int SUMMARY_ID = 9999;
    
    private ListenerRegistration alertsListener;
    private FirebaseFirestore db;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sedentaryCheckRunnable;
    private String currentElderUid;
    
    private final Map<String, Long> lastNotificationTimes = new ConcurrentHashMap<>();
    private final Map<String, String> lastAlertMessages = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = 30 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        UserSessionManager.getInstance().init(this);
        db = FirebaseFirestore.getInstance();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        currentElderUid = UserSessionManager.getInstance().getLinkedElderId();
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ElderCare Monitor Active")
                .setContentText("Monitoring health data for alerts...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(SERVICE_NOTIFICATION_ID, notification);

        if (currentElderUid != null) {
            startAlertListener(currentElderUid);
            startSedentaryMonitoring();
        } else {
            Log.w(TAG, "No linked elder ID found. Alert listener not started.");
        }

        return START_STICKY;
    }

    private void startAlertListener(String elderUid) {
        if (alertsListener != null) {
            alertsListener.remove();
            ListenerRegistry.INSTANCE.unregister("service_alerts_listener");
        }

        // Look back 10 minutes to account for server/device clock drift
        Timestamp serviceStartTime = new Timestamp(new java.util.Date(System.currentTimeMillis() - 600000));

        // Simplified query - filter isAcknowledged on client side to avoid composite index
        alertsListener = db.collection("users").document(elderUid).collection("alerts")
                .whereEqualTo("isResolved", false)
                .whereGreaterThan("timestamp", serviceStartTime)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        Log.e(TAG, "Alert listener error: ", error);
                        return;
                    }

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Boolean isAcknowledged = dc.getDocument().getBoolean("isAcknowledged");
                        if (Boolean.TRUE.equals(isAcknowledged)) continue;
                        
                        String docId = dc.getDocument().getId();
                        String title = dc.getDocument().getString("title");
                        String message = dc.getDocument().getString("message");
                        String type = dc.getDocument().getString("type");

                        if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                            if (shouldNotify(docId, type, message)) {
                                sendEmergencyNotification(elderUid, type, title, message);
                            }
                        }
                    }
                });
        
        // Fix 1: Register in centralized registry to ensure detachment on logout
        ListenerRegistry.INSTANCE.register("service_alerts_listener", alertsListener);
    }

    private boolean shouldNotify(String docId, String type, String message) {
        String lastMsg = lastAlertMessages.get(docId);
        if (message != null && message.equals(lastMsg)) return false;

        String key = currentElderUid + "_" + type;
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastNotificationTimes.get(key);
        
        if (lastMsg != null && !lastMsg.equals(message)) {
            lastAlertMessages.put(docId, message != null ? message : "");
            lastNotificationTimes.put(key, currentTime);
            return true;
        }

        if (lastTime != null && (currentTime - lastTime < RATE_LIMIT_MS)) return false;
        
        lastNotificationTimes.put(key, currentTime);
        lastAlertMessages.put(docId, message != null ? message : "");
        return true;
    }

    private void sendEmergencyNotification(String elderUid, String type, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        int notificationId = (elderUid + type).hashCode();

        Intent intent = new Intent(this, CaregiverMainActivity.class);
        intent.putExtra("open_alerts", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        PendingIntent callPendingIntent = PendingIntent.getActivity(this, notificationId + 1, callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent viewIntent = new Intent(this, CaregiverMainActivity.class);
        viewIntent.putExtra("open_alerts", true);
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(this, notificationId + 2, viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String shortMessage = message;
        if (message != null && message.length() > 100) {
            shortMessage = message.substring(0, 97) + "...";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_warning_24)
                .setContentTitle(title != null ? title : "Safety Alert")
                .setContentText(shortMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setGroup(GROUP_KEY_ALERTS)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.baseline_call_24, "Call", callPendingIntent)
                .addAction(android.R.drawable.ic_menu_view, "View", viewPendingIntent);

        // Fix 3: Silent Group Summary to prevent muting individual alerts
        Notification summaryNotification = new NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
                .setContentTitle("Eldercare Alerts")
                .setSmallIcon(R.drawable.baseline_warning_24)
                .setGroup(GROUP_KEY_ALERTS)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true) 
                .setAutoCancel(true)
                .build();

        notificationManager.notify(notificationId, builder.build());
        notificationManager.notify(SUMMARY_ID, summaryNotification);
    }

    private void startSedentaryMonitoring() {
        // TEMPORARILY DISABLED FOR DEMO - Prevents false positives on new accounts
        // if (sedentaryCheckRunnable != null) handler.removeCallbacks(sedentaryCheckRunnable);
        // sedentaryCheckRunnable = new Runnable() {
        //     @Override
        //     public void run() {
        //         checkSedentaryStatus();
        //         handler.postDelayed(this, 30 * 60 * 1000);
        //     }
        // };
        // handler.post(sedentaryCheckRunnable);
        Log.d(TAG, "Sedentary monitoring disabled for demo");
    }

    private void checkSedentaryStatus() {
        if (currentElderUid == null) return;
        long fourHoursAgo = System.currentTimeMillis() - (4 * 60 * 60 * 1000);
        Timestamp tsThreshold = new Timestamp(new java.util.Date(fourHoursAgo));

        db.collection("users").document(currentElderUid).collection("activityLogs")
                .whereGreaterThan("timestamp", tsThreshold)
                .limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        triggerSedentaryAlert();
                    }
                });
    }

    private void triggerSedentaryAlert() {
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", "Sedentary Warning");
        alert.put("message", "The elder hasn't shown activity for over 4 hours.");
        alert.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        alert.put("isResolved", false);
        alert.put("isAcknowledged", false);
        alert.put("type", "sedentary");
        db.collection("users").document(currentElderUid).collection("alerts").add(alert);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Monitor Service", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(serviceChannel);

            NotificationChannel emergencyChannel = new NotificationChannel(
                    EMERGENCY_CHANNEL_ID, "Health Alerts", NotificationManager.IMPORTANCE_HIGH);
            emergencyChannel.enableVibration(true);
            emergencyChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(emergencyChannel);
        }
    }

    @Override
    public void onDestroy() {
        if (alertsListener != null) {
            alertsListener.remove();
            ListenerRegistry.INSTANCE.unregister("service_alerts_listener");
        }
        if (sedentaryCheckRunnable != null) handler.removeCallbacks(sedentaryCheckRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
