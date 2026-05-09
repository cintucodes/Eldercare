# Health Connect Integration Testing Guide

## Overview
This guide shows how to test the complete Health Connect integration **without a physical wearable device** using the built-in test data generator.

---

## ✅ What's Implemented

### 1. **Test Data Generator** (`HealthConnectTestDataGenerator.java`)
- Generates realistic health vitals that simulate wearable device data
- Three test scenarios: Normal, Full Day, Abnormal
- Automatically saves to Firebase with `source: "Health Connect"`
- Updates `latest_vitals_entry` for real-time display

### 2. **Daily Sync Reminder** (`HealthSyncReminderWorker.java`)
- Runs every 24 hours via WorkManager
- Checks if elder hasn't synced in 24 hours
- Sends notification: "Time to Sync Health Data"
- Tapping notification opens Profile tab

### 3. **Enhanced Sync Button** (ElderProfileFragment)
- Shows test scenario menu when clicked
- Three options: Normal Data, Full Day, Abnormal Data
- Simulates realistic sync delay (1.5 seconds)
- Updates `lastHealthConnectSync` timestamp in Firebase

---

## 🧪 Testing Instructions

### **Test 1: Normal Health Data Sync**

1. **Login as Elder**
2. **Navigate to Profile tab**
3. **Tap "Sync from Health Connect"**
4. **Select "Normal Data"** from dialog
5. **Wait for sync** (shows "Syncing..." then completes)
6. **Verify success message**: "✓ Synced 5 vitals from Health Connect!"

**Expected Results:**
- Heart Rate: 70-80 bpm (normal)
- Steps: 7,000-10,000 (healthy)
- Blood Glucose: 90-110 mg/dL (normal)
- Sleep: 7-8 hours (good)
- Blood Pressure: 120/80 (normal)

**Where to Check:**
- Home tab → Shows latest vitals
- Health tab → Shows vitals history
- Caregiver app → Sees new data in real-time
- **No alerts triggered** (all values normal)

---

### **Test 2: Full Day Realistic Data**

1. **Tap "Sync from Health Connect"**
2. **Select "Full Day"**
3. **Wait for sync**

**Expected Results:**
- **10 vitals saved** (morning, midday, evening, sleep)
- Realistic patterns:
  - Morning: Lower heart rate (65-75 bpm), fewer steps
  - Midday: Higher heart rate (70-85 bpm), more steps
  - Evening: Moderate heart rate, peak steps (6,000-9,000)
  - Sleep: 6.5-8.5 hours

**Where to Check:**
- Health tab → Multiple entries with different timestamps
- Caregiver app → Activity feed shows all entries
- Charts show realistic daily progression

---

### **Test 3: Abnormal Data (Alert Testing)**

1. **Tap "Sync from Health Connect"**
2. **Select "Abnormal"**
3. **Wait for sync**

**Expected Results:**
- Heart Rate: **105 bpm** (HIGH - triggers RED alert)
- Sleep: **4 hours** (LOW - triggers AMBER alert)
- Blood Glucose: **180 mg/dL** (HIGH - no alert, just logged)
- Steps: 2,000 (low but no alert)

**Caregiver Receives:**
- 🔴 **RED Alert**: "High Heart Rate Detected - 105 bpm"
- 🟠 **AMBER Alert**: "Low Sleep Duration - 4.0 hours"
- Push notifications with action buttons (Call, View)

**Where to Check:**
- Caregiver app → Alerts tab shows 2 new alerts
- Caregiver notifications → 2 push notifications
- Elder app → Data logged normally (no alerts shown to elder)

---

### **Test 4: Daily Sync Reminder**

**Setup:**
1. **Sync data once** (any scenario)
2. **Wait 24 hours** OR **manually trigger** (see below)

**Manual Trigger (for immediate testing):**
```java
// In ElderProfileFragment, add temporary test button:
binding.btnTestReminder.setOnClickListener(v -> {
    // Update lastHealthConnectSync to 25 hours ago
    long yesterday = System.currentTimeMillis() - (25 * 60 * 60 * 1000);
    db.collection("users").document(uid)
        .update("lastHealthConnectSync", yesterday)
        .addOnSuccessListener(x -> {
            // Manually trigger worker
            WorkManager.getInstance(requireContext())
                .enqueue(OneTimeWorkRequest.from(HealthSyncReminderWorker.class));
        });
});
```

**Expected Results:**
- Notification appears: "Time to Sync Health Data"
- Message: "Haven't synced in 24 hours. Tap to sync from your wearable."
- Tapping notification → Opens Profile tab
- Auto-cancels after tapping

---

## 📊 Data Flow Verification

### **Firebase Structure After Sync:**

```
users/{elderId}/
  - lastHealthConnectSync: 1737849600000 (timestamp)
  
  vitals/
    {autoId1}/
      - type: "heartRate"
      - value: 72
      - timestamp: Timestamp
      - source: "Health Connect"
    
    {autoId2}/
      - type: "steps"
      - value: 8500
      - timestamp: Timestamp
      - source: "Health Connect"
    
    latest_vitals_entry/
      - heartRate: 72
      - steps: 8500
      - bloodGlucose: 95
      - sleepHours: 7.5
      - systolicBP: 120
      - diastolicBP: 80
      - lastUpdated: Timestamp
```

### **Verify in Firebase Console:**
1. Go to Firestore Database
2. Navigate to `users/{elderId}/vitals`
3. Check for documents with `source: "Health Connect"`
4. Verify `latest_vitals_entry` exists
5. Check `lastHealthConnectSync` timestamp in user document

---

## 🔍 Troubleshooting

### **Issue: Sync button does nothing**
- Check Logcat for errors
- Verify Firebase connection
- Ensure user is logged in (uid not null)

### **Issue: No alerts triggered with abnormal data**
- Check RuleEngine.java is enabled
- Verify CaregiverAlertService is running (caregiver side)
- Check caregiver is linked to elder
- Verify notification permissions granted

### **Issue: Reminder notification not appearing**
- Check WorkManager is scheduled: `adb shell dumpsys jobscheduler`
- Verify `lastHealthConnectSync` timestamp is >24 hours old
- Check notification permissions granted
- Verify user role is "elder" in Firestore

### **Issue: Data not showing in Home tab**
- Check `latest_vitals_entry` document exists
- Verify ElderHomeFragment is reading from correct path
- Check Firebase offline persistence is enabled
- Refresh by switching tabs

---

## 🎯 Demo Scenarios

### **Scenario 1: Healthy Elder (Caregiver Peace of Mind)**
1. Sync "Normal Data" daily
2. Caregiver sees consistent healthy vitals
3. No alerts triggered
4. Shows system working smoothly

### **Scenario 2: Health Concern (Alert System)**
1. Sync "Abnormal Data"
2. Caregiver immediately receives RED/AMBER alerts
3. Caregiver can call elder or view details
4. Demonstrates real-time monitoring

### **Scenario 3: Daily Routine (Full Day Pattern)**
1. Sync "Full Day" data
2. Shows realistic activity patterns
3. Caregiver sees elder's daily rhythm
4. Demonstrates comprehensive tracking

---

## 📱 Real Wearable Integration (Future)

When a physical wearable is available:

1. **Install Health Connect** from Play Store
2. **Pair wearable** (Fitbit, Samsung Watch, Garmin, etc.)
3. **Grant permissions** via "Grant Permissions" button
4. **Sync button** will pull REAL data instead of test data
5. **Background sync** can be enabled (WorkManager every 4 hours)

**Current Implementation:**
- ✅ Permission request flow ready
- ✅ HealthConnectManager.kt has all read methods
- ✅ Data saving logic complete
- ⏳ Actual permission request needs ActivityResultLauncher
- ⏳ Background sync disabled (manual only)

---

## ✨ Key Features Demonstrated

1. ✅ **Realistic test data** without hardware
2. ✅ **Three test scenarios** for different use cases
3. ✅ **Alert system integration** (abnormal data triggers alerts)
4. ✅ **Daily reminder** (24-hour nudge to sync)
5. ✅ **Firebase sync** (data appears in caregiver app)
6. ✅ **Source tracking** (all marked "Health Connect")
7. ✅ **Timestamp tracking** (prevents duplicate reminders)

---

## 🚀 Next Steps

1. **Test all three scenarios** to verify data flow
2. **Link caregiver account** to test alert delivery
3. **Wait 24 hours** (or manually trigger) to test reminder
4. **Check Firebase Console** to verify data structure
5. **Demo to stakeholders** using test scenarios

---

**Note**: This implementation allows full testing and demonstration of Health Connect integration without requiring physical wearable devices. The test data generator creates realistic patterns that showcase the app's monitoring and alert capabilities.
