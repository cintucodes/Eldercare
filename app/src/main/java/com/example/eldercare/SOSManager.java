package com.example.eldercare;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Manages SOS emergency alerts.
 * SECURITY: Identity-locked to prevent Caregiver devices from sending local SMS.
 */
public class SOSManager {
    private static final String TAG = "SOSManager";
    public static final String ACTION_SMS_SENT = "com.example.eldercare.SMS_SENT";
    
    private static SOSManager instance;
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore db;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int LOCATION_TIMEOUT_MS = 10000; // Increased to 10 seconds for better GPS lock
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SOSManager(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
        this.db = FirebaseFirestore.getInstance();
        UserSessionManager.getInstance().init(this.context);
        registerSmsStatusReceivers();
    }

    public static synchronized SOSManager getInstance(Context context) {
        if (instance == null) {
            instance = new SOSManager(context);
        }
        return instance;
    }

    private void registerSmsStatusReceivers() {
        BroadcastReceiver statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_SMS_SENT.equals(intent.getAction())) {
                    if (getResultCode() != android.app.Activity.RESULT_OK) {
                        Log.e(TAG, "SMS Status: Failed to send. Error code: " + getResultCode());
                    } else {
                        Log.i(TAG, "SMS Status: Successfully sent via default SIM.");
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_SMS_SENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(statusReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(statusReceiver, filter);
        }
    }

    /**
     * Verifies if the device is capable of sending SMS alerts.
     * Suggested Improvement 4: Connectivity check before SMS
     */
    public boolean canSendSms() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null && tm.getSimState() == TelephonyManager.SIM_STATE_READY;
    }
    
    /**
     * Checks if location services are enabled on the device.
     */
    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return false;
        
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        return gpsEnabled || networkEnabled;
    }

    public void triggerSOS(String targetUid) {
        UserSessionManager session = UserSessionManager.getInstance();
        session.init(context);

        if (session.isCaregiver()) {
            Log.w(TAG, "SOS Blocked: Current device is in Caregiver mode.");
            return;
        }

        if (targetUid == null || !targetUid.equals(session.getUid())) {
            Log.e(TAG, "SOS Blocked: UID Mismatch");
            return;
        }

        Log.d(TAG, "Triggering SOS for: " + targetUid);

        // Create Alert in Firestore with unique ID
        final DocumentReference alertRef = db.collection("users").document(targetUid).collection("alerts").document();
        final String alertId = alertRef.getId();
        
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", "SOS EMERGENCY!");
        alert.put("message", "Emergency SOS triggered by " + (UserSessionManager.getInstance().getFullName() != null ? UserSessionManager.getInstance().getFullName() : "Elder") + ". Getting location...");
        alert.put("type", "health_sos");
        alert.put("severity", "critical");
        alert.put("isResolved", false);
        alert.put("isAcknowledged", false);
        alert.put("timestamp", FieldValue.serverTimestamp());
        alert.put("elderId", targetUid);
        alert.put("alertId", alertId);
        alertRef.set(alert);

        // Fetch location for THIS specific alert - create new instance for each call
        fetchBestLocationForAlert(alertId, location -> {
            if (location != null) {
                getAddressAsync(location.getLatitude(), location.getLongitude(), addressInfo -> {
                    updateAlertAndNotify(alertRef, location, addressInfo, targetUid, alertId);
                });
            } else {
                updateAlertAndNotify(alertRef, null, "Location Unavailable", targetUid, alertId);
            }
        });
    }

    private void fetchBestLocationForAlert(String alertId, Consumer<android.location.Location> callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "[Alert " + alertId + "] Location permission not granted");
            callback.accept(null);
            return;
        }

        // Check if location services are enabled
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (!gpsEnabled && !networkEnabled) {
            Log.e(TAG, "[Alert " + alertId + "] Location services are disabled");
            callback.accept(null);
            return;
        }

        Log.d(TAG, "[Alert " + alertId + "] Fetching location - GPS: " + gpsEnabled + ", Network: " + networkEnabled);
        
        // Create NEW state for THIS alert to avoid race conditions
        final LocationFetchState state = new LocationFetchState();
        
        // First, get last known location as immediate fallback
        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
            if (lastLoc != null) {
                long ageSeconds = (System.currentTimeMillis() - lastLoc.getTime()) / 1000;
                Log.i(TAG, "[Alert " + alertId + "] Found cached location: " + lastLoc.getLatitude() + ", " + lastLoc.getLongitude() + " (age: " + ageSeconds + "s, accuracy: " + lastLoc.getAccuracy() + "m)");
                state.bestLocation = lastLoc;
                
                // If cached location is recent (< 5 minutes) and accurate, use it immediately
                if (ageSeconds < 300 && lastLoc.getAccuracy() < 100 && !state.callbackInvoked) {
                    state.callbackInvoked = true;
                    Log.i(TAG, "[Alert " + alertId + "] Using recent cached location (good accuracy)");
                    callback.accept(lastLoc);
                    return;
                }
            } else {
                Log.w(TAG, "[Alert " + alertId + "] No cached location available");
            }
            
            // If no good cached location, try to get fresh location
            if (!state.callbackInvoked) {
                requestFreshLocationForAlert(alertId, callback, state);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "[Alert " + alertId + "] Failed to get last location: " + e.getMessage());
            requestFreshLocationForAlert(alertId, callback, state);
        });
    }
    
    private void requestFreshLocationForAlert(String alertId, Consumer<android.location.Location> callback, LocationFetchState state) {
        // Set timeout to use best available location
        Runnable timeoutTask = () -> {
            if (!state.callbackInvoked) {
                state.callbackInvoked = true;
                if (state.bestLocation != null) {
                    Log.i(TAG, "[Alert " + alertId + "] Timeout - using best available location: " + state.bestLocation.getLatitude() + ", " + state.bestLocation.getLongitude());
                    callback.accept(state.bestLocation);
                } else {
                    Log.e(TAG, "[Alert " + alertId + "] Timeout - no location available at all");
                    callback.accept(null);
                }
            }
        };
        mainHandler.postDelayed(timeoutTask, LOCATION_TIMEOUT_MS);

        // Try current location with high accuracy
        CurrentLocationRequest highAccuracyRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(LOCATION_TIMEOUT_MS)
                .setMaxUpdateAgeMillis(300000) // Accept location up to 5min old
                .build();

        fusedLocationClient.getCurrentLocation(highAccuracyRequest, null)
            .addOnSuccessListener(location -> {
                if (location != null && !state.callbackInvoked) {
                    mainHandler.removeCallbacks(timeoutTask);
                    state.callbackInvoked = true;
                    Log.i(TAG, "[Alert " + alertId + "] Got current high-accuracy location: " + location.getLatitude() + ", " + location.getLongitude() + " (accuracy: " + location.getAccuracy() + "m)");
                    callback.accept(location);
                } else if (location != null) {
                    // Update best location even if callback already invoked
                    state.bestLocation = location;
                    Log.d(TAG, "[Alert " + alertId + "] Got location but callback already invoked");
                } else {
                    Log.w(TAG, "[Alert " + alertId + "] High accuracy location returned null, waiting for timeout with fallback...");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "[Alert " + alertId + "] Failed to get current location: " + e.getMessage());
                // Don't invoke callback here, let timeout handle it with bestLocation
            });
    }
    
    // Helper class to encapsulate location fetch state for each alert
    private static class LocationFetchState {
        boolean callbackInvoked = false;
        android.location.Location bestLocation = null;
    }

    private void updateAlertAndNotify(DocumentReference ref, android.location.Location loc, String address, String uid, String alertId) {
        Map<String, Object> updates = new HashMap<>();
        String name = UserSessionManager.getInstance().getFullName();
        if (name == null || name.isEmpty()) name = "Elder User";

        String message;
        if (loc != null) {
            updates.put("latitude", loc.getLatitude());
            updates.put("longitude", loc.getLongitude());
            updates.put("locationString", address);
            message = "EMERGENCY: " + name + " needs help at " + address + ". View location.";
            Log.i(TAG, "Alert " + alertId + " updated with location: " + address);
        } else {
            updates.put("locationString", "Location Unavailable");
            message = "EMERGENCY: " + name + " needs help! Location unavailable.";
            Log.w(TAG, "Alert " + alertId + " - location unavailable");
        }

        updates.put("message", message);
        ref.update(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Alert " + alertId + " successfully updated");
            } else {
                Log.e(TAG, "Failed to update alert " + alertId + ": " + task.getException());
            }
            
            UserSessionManager session = UserSessionManager.getInstance();
            if (session.isElder() && uid.equals(session.getUid())) {
                sendFallbackSMS(uid, message);
            }
            sendFCMAlert(uid, message);
        });
    }

    private void getAddressAsync(double lat, double lng, Consumer<String> callback) {
        executor.execute(() -> {
            String addressText = String.format(Locale.US, "%.4f, %.4f", lat, lng);
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (addr.getLocality() != null) sb.append(addr.getLocality()).append(", ");
                    if (addr.getSubAdminArea() != null) sb.append(addr.getSubAdminArea()).append(", ");
                    if (addr.getAdminArea() != null) sb.append(addr.getAdminArea());
                    String res = sb.toString().trim();
                    if (res.endsWith(",")) res = res.substring(0, res.length() - 1);
                    if (!res.isEmpty()) addressText = res;
                    else addressText = addr.getAddressLine(0);
                }
            } catch (Exception e) {
                Log.w(TAG, "Geocoding error: " + e.getMessage());
            }
            final String finalAddress = addressText;
            mainHandler.post(() -> callback.accept(finalAddress));
        });
    }

    private void sendFallbackSMS(String uid, String message) {
        UserSessionManager session = UserSessionManager.getInstance();
        
        // HARDWARE ACCESS GATE: Prevent Caregiver devices from sending SMS locally.
        if (!session.isElder() || !uid.equals(session.getUid())) {
            Log.e(TAG, "FATAL SECURITY: Attempt to send SMS from unauthorized device role: " + session.getRole());
            return;
        }

        // Suggested Improvement 4: Verify connectivity before sending
        if (!canSendSms()) {
            Log.e(TAG, "SMS Fallback Aborted: Device not capable or SIM not ready.");
            return;
        }

        // Try getting phone number from session first (faster)
        String cachedPhone = session.getLinkedPhone();
        if (cachedPhone != null && !cachedPhone.isEmpty()) {
            executeSmsSending(cachedPhone, message);
        } else {
            // Fallback to fetching from Firestore if session cache is missing
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                String contact = doc.getString("emergencyContactPhone");
                if (contact == null || contact.isEmpty()) contact = doc.getString("caregiverPhone");
                if (contact != null && !contact.isEmpty()) {
                    executeSmsSending(contact, message);
                } else {
                    Log.e(TAG, "No emergency contact phone number found for elder: " + uid);
                }
            });
        }
    }

    private void executeSmsSending(String contact, String message) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS Permission not granted. Cannot send SOS.");
            return;
        }

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+: Correct way to get the default manager via System Service
                smsManager = context.getSystemService(SmsManager.class);
                if (smsManager != null) {
                    int subId = SmsManager.getDefaultSmsSubscriptionId();
                    if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        smsManager = smsManager.createForSubscriptionId(subId);
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                // API 22-30: Use the default SMS subscription ID for dual-SIM support
                int subId = SmsManager.getDefaultSmsSubscriptionId();
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
                } else {
                    smsManager = SmsManager.getDefault();
                }
            } else {
                // API < 22: Traditional default
                smsManager = SmsManager.getDefault();
            }

            if (smsManager == null) {
                Log.e(TAG, "SmsManager unavailable on this device.");
                return;
            }

            ArrayList<String> parts = smsManager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            
            // Create a tracking intent for each part
            Intent sentIntent = new Intent(ACTION_SMS_SENT);
            PendingIntent pi = PendingIntent.getBroadcast(context, (int)System.currentTimeMillis(), sentIntent, 
                    PendingIntent.FLAG_IMMUTABLE);
            
            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(pi);
            }

            smsManager.sendMultipartTextMessage(contact, null, parts, sentIntents, null);
            Log.i(TAG, "SOS SMS sent successfully FROM ELDER'S PHONE using default SIM to: " + contact);
        } catch (Exception e) {
            Log.e(TAG, "SMS transmission failed: " + e.getMessage());
        }
    }

    private void sendFCMAlert(String uid, String message) {
        String name = UserSessionManager.getInstance().getFullName();
        if (name == null || name.isEmpty()) name = "Elder";
        
        String shortMessage = name + " triggered SOS. Check alerts for details.";
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("topic", "alerts_" + uid);
        notification.put("title", "🆘 SOS EMERGENCY");
        notification.put("body", shortMessage);
        notification.put("timestamp", FieldValue.serverTimestamp());
        notification.put("type", "sos_alert");
        db.collection("notifications").add(notification);
    }
}
