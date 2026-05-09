package com.example.eldercare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

public class MedicationReminderScheduler {
    private static final String TAG = "MedReminderScheduler";

    public static void scheduleAlarms(Context context, String medId, String name, String dosage, List<String> times) {
        if (times == null) return;
        for (int i = 0; i < times.size(); i++) {
            scheduleAlarm(context, medId, name, dosage, times.get(i), i);
        }
    }

    public static void scheduleAlarm(Context context, String medId, String name, String dosage, String time, int index) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String[] parts = time.split(":");
        if (parts.length != 2) return;
        
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, MedicationReminderReceiver.class);
        intent.putExtra("medId", medId);
        intent.putExtra("medName", name);
        intent.putExtra("dosage", dosage);
        intent.putExtra("time", time);
        intent.putExtra("reminderIndex", index);

        int requestCode = (medId + index).hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calendar.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use setExactAndAllowWhileIdle to fire even in Doze mode
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
        
        Log.d(TAG, "Scheduled alarm for " + name + " at " + time + " (Exact)");
    }

    public static void cancelAlarms(Context context, String medId, int timesCount) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int i = 0; i < timesCount; i++) {
            Intent intent = new Intent(context, MedicationReminderReceiver.class);
            int requestCode = (medId + i).hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    requestCode, 
                    intent, 
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }
}
