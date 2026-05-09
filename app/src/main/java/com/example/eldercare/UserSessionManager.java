package com.example.eldercare;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class UserSessionManager {
    private static UserSessionManager instance;
    private static final String PREF_NAME = "ElderCareSession";
    private static final String KEY_UID = "uid";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_ELDER_ID = "linkedElderId";
    private static final String KEY_CAREGIVER_ID = "linkedCaregiverId";
    private static final String KEY_PHONE = "linkedPhone";

    private String uid;
    private String role;
    private String firstName;
    private String lastName;
    private String linkedElderId;
    private String linkedCaregiverId;
    private String linkedPhone;

    private UserSessionManager() {}

    public static synchronized UserSessionManager getInstance() {
        if (instance == null) {
            instance = new UserSessionManager();
        }
        return instance;
    }

    public void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.uid = prefs.getString(KEY_UID, null);
        this.role = prefs.getString(KEY_ROLE, null);
        this.firstName = prefs.getString(KEY_FIRST_NAME, null);
        this.lastName = prefs.getString(KEY_LAST_NAME, null);
        this.linkedElderId = prefs.getString(KEY_ELDER_ID, null);
        this.linkedCaregiverId = prefs.getString(KEY_CAREGIVER_ID, null);
        this.linkedPhone = prefs.getString(KEY_PHONE, null);
    }

    public void setSession(Context context, String uid, @Nullable String role, 
                           @Nullable String firstName, @Nullable String lastName,
                           @Nullable String linkedElderId, @Nullable String linkedCaregiverId) {
        
        // Unsubscribe from old topics if switching users to prevent notification leakage
        if (this.uid != null && !this.uid.equals(uid)) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("user_" + this.uid);
            if (this.linkedElderId != null) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("alerts_" + this.linkedElderId);
            }
        }

        this.uid = uid;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.linkedElderId = linkedElderId;
        this.linkedCaregiverId = linkedCaregiverId;

        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        safePutString(editor, KEY_UID, uid);
        safePutString(editor, KEY_ROLE, role);
        safePutString(editor, KEY_FIRST_NAME, firstName);
        safePutString(editor, KEY_LAST_NAME, lastName);
        safePutString(editor, KEY_ELDER_ID, linkedElderId);
        safePutString(editor, KEY_CAREGIVER_ID, linkedCaregiverId);
        editor.apply();
        
        // Sync FCM topics for the current user
        if (uid != null) {
            FirebaseMessaging.getInstance().subscribeToTopic("user_" + uid);
            if ("caregiver".equals(role) && linkedElderId != null) {
                FirebaseMessaging.getInstance().subscribeToTopic("alerts_" + linkedElderId);
            }
        }
    }

    public void updateName(Context context, @Nullable String firstName, @Nullable String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        safePutString(editor, KEY_FIRST_NAME, firstName);
        safePutString(editor, KEY_LAST_NAME, lastName);
        editor.apply();
    }

    private void safePutString(SharedPreferences.Editor editor, String key, @Nullable String value) {
        if (value != null) {
            editor.putString(key, value);
        } else {
            editor.remove(key);
        }
    }

    public void setLinkedElderId(Context context, @Nullable String linkedElderId) {
        // Unsubscribe from the old elder's alerts before switching
        if (this.linkedElderId != null && !this.linkedElderId.equals(linkedElderId)) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("alerts_" + this.linkedElderId);
        }

        this.linkedElderId = linkedElderId;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        safePutString(editor, KEY_ELDER_ID, linkedElderId);
        editor.apply();

        // Ensure caregiver subscribes to the newly linked elder's alerts
        if (linkedElderId != null && isCaregiver()) {
            FirebaseMessaging.getInstance().subscribeToTopic("alerts_" + linkedElderId);
        }
    }

    public void setLinkedPhone(Context context, @Nullable String phone) {
        this.linkedPhone = phone;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        safePutString(editor, KEY_PHONE, phone);
        editor.apply();
    }

    public boolean isLoggedIn() { return uid != null; }
    public String getUid() { return uid; }
    public String getRole() { return role; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { 
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (lastName != null && !lastName.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName);
        }
        return sb.toString().trim();
    }
    public String getLinkedElderId() { return linkedElderId; }
    public String getLinkedCaregiverId() { return linkedCaregiverId; }
    public String getLinkedPhone() { return linkedPhone; }

    public boolean isCaregiver() { return "caregiver".equals(role); }
    public boolean isElder() { return "elder".equals(role); }

    public void logout(Context context) {
        Log.d("UserSessionManager", "Performing centralized logout cleanup...");
        
        // 1. Remove all active Firestore listeners
        ListenerRegistry.INSTANCE.removeAll();

        // 2. Unsubscribe from FCM topics and invalidate token
        if (uid != null) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("user_" + this.uid);
            if (this.linkedElderId != null) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("alerts_" + this.linkedElderId);
            }
            // Recommended: Delete token on logout to ensure security
            FirebaseMessaging.getInstance().deleteToken();
        }

        // 3. Stop monitoring services
        context.stopService(new Intent(context, CaregiverAlertService.class));
        context.stopService(new Intent(context, ElderMonitoringService.class)); // Fix Improvement 2

        // 4. Cancel background synchronization
        try {
            WorkManager.getInstance(context).cancelUniqueWork("ElderCareSync");
            Log.d("UserSessionManager", "Background sync cancelled during logout");
        } catch (Exception e) {
            Log.e("UserSessionManager", "Failed to cancel work: " + e.getMessage());
        }

        // 5. Clear all active notifications
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancelAll();

        // 6. Clear session and Firebase Auth
        clearSession(context);
        FirebaseAuth.getInstance().signOut();

        // 7. Navigate to Login
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void clearSession(Context context) {
        uid = null;
        role = null;
        firstName = null;
        lastName = null;
        linkedElderId = null;
        linkedCaregiverId = null;
        linkedPhone = null;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
