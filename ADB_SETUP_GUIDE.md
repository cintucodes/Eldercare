# Health Connect ADB Permission Setup

## Quick Setup (2 minutes)

### Step 1: Connect Your Device
1. Enable USB Debugging on your Android device
2. Connect device to computer via USB
3. Verify connection: Open terminal and run `adb devices`
   - You should see your device listed

### Step 2: Install the App
1. Build and install your app from Android Studio
2. Make sure the app is installed before granting permissions

### Step 3: Grant Permissions via ADB
1. Double-click `grant_health_permissions.bat` in the project root
2. Wait for "All Health Connect permissions granted!" message
3. Press any key to close

### Step 4: Verify in App
1. Open ElderCare app
2. Go to Profile tab
3. You should see: "Status: ✓ All Permissions Granted (14/14)"
4. Now you can use "Generate Test Data" and "Sync from Health Connect" buttons

## Troubleshooting

**"device not found"**
- Run `adb devices` to check connection
- Make sure USB debugging is enabled
- Try different USB cable/port

**"Unknown package: com.example.eldercare"**
- Install the app first from Android Studio
- Make sure package name matches (check AndroidManifest.xml)

**Permissions not showing in app**
- Close and reopen the app
- Check logcat for permission errors

## For Presentation Demo

1. Run the batch script BEFORE your presentation
2. Keep the device connected during demo
3. If permissions are lost, just run the script again (takes 5 seconds)

## Manual ADB Commands (if batch file doesn't work)

```bash
adb shell pm grant com.example.eldercare android.permission.health.READ_STEPS
adb shell pm grant com.example.eldercare android.permission.health.WRITE_STEPS
adb shell pm grant com.example.eldercare android.permission.health.READ_HEART_RATE
adb shell pm grant com.example.eldercare android.permission.health.WRITE_HEART_RATE
adb shell pm grant com.example.eldercare android.permission.health.READ_BLOOD_PRESSURE
adb shell pm grant com.example.eldercare android.permission.health.WRITE_BLOOD_PRESSURE
adb shell pm grant com.example.eldercare android.permission.health.READ_BLOOD_GLUCOSE
adb shell pm grant com.example.eldercare android.permission.health.WRITE_BLOOD_GLUCOSE
adb shell pm grant com.example.eldercare android.permission.health.READ_SLEEP
adb shell pm grant com.example.eldercare android.permission.health.WRITE_SLEEP
adb shell pm grant com.example.eldercare android.permission.health.READ_DISTANCE
adb shell pm grant com.example.eldercare android.permission.health.WRITE_DISTANCE
adb shell pm grant com.example.eldercare android.permission.health.READ_ACTIVE_CALORIES_BURNED
adb shell pm grant com.example.eldercare android.permission.health.WRITE_ACTIVE_CALORIES_BURNED
```
