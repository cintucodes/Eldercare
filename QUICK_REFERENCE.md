# Quick Reference: Testing & Debugging

## 🚨 SOS Location Testing

### Test Steps
1. Trigger SOS alert (press & hold button)
2. Wait 3 seconds
3. Trigger another SOS alert
4. Open Caregiver app → Alerts tab

### ✅ Success
- Both alerts show location OR "Location Unavailable"
- No alert stuck at "Fetching location..."

### 🔍 Debug Logs
```
[Alert abc] Fetching location
[Alert abc] Got location: 37.7749, -122.4194
[Alert xyz] Fetching location
[Alert xyz] Got location: 37.7749, -122.4194
```

---

## 👥 Caregiver Display Testing

### Test Steps
1. Tap "Linked Caregivers" card
2. Observe dialog

### ✅ Success
- Dialog shows caregiver name, phone, relationship
- "Call" button works
- "Generate New Code" opens ElderInviteActivity

### 🔍 Debug Logs
```
Caregivers subcollection query returned 1 documents
Loaded caregiver: John Doe (Son)
```

---

## 🔢 Code Generation Testing

### Test Steps
1. Tap "Link Caregiver" button OR
2. Tap "Linked Caregivers" → "Generate New Code"
3. Tap "Generate New Code" button
4. Tap "Copy Code"

### ✅ Success
- 6-digit code appears (e.g., "123456")
- "Expires in 48 hours" shows
- Code copies to clipboard

### 🔍 Debug Logs
```
ElderInviteActivity: onCreate - userId: uid123
LinkingCodeManager: Generated code: 123456
Code saved successfully to Firestore
```

---

## ⚠️ Common Issues

| Issue | Log Message | Fix |
|-------|-------------|-----|
| User not logged in | `userId is null!` | Log out/in |
| No caregivers | `linkedCaregivers: null` | Link a caregiver |
| Firestore error | `PERMISSION_DENIED` | Check Firestore rules |
| Location disabled | `Location services disabled` | Enable GPS |
| Activity not opening | No logs | Check AndroidManifest.xml |

---

## 📱 SMS "Error" (Ignore This)

```
SMSDispatcher: enter persistSentMessageIfRequired()
java.lang.Throwable
```

**This is NOT an error!** It's normal Android system logging. Ignore it.

---

## 🔧 Quick Fixes

### Fix 1: Clear App Data
Settings → Apps → ElderCare → Storage → Clear Data

### Fix 2: Re-login
Log out → Log in again

### Fix 3: Check Internet
Ensure device has internet connection

---

## 📊 Firestore Data Check

### Elder Document
```
users/{elderId}/linkedCaregivers: ["cgId1"]
```

### Caregiver Subcollection
```
users/{elderId}/caregivers/{cgId1}/relationship: "Son"
```

### Linking Code
```
linkingCodes/123456/elderId: "uid123"
```

---

## 📝 Logcat Filter

Use this filter to see only relevant logs:
```
tag:SOSManager|ElderHomeFragment|ElderInviteActivity|LinkingCodeManager
```

---

## ✅ All Tests Passing?

- [ ] Multiple SOS alerts show independent locations
- [ ] Code generation creates 6-digit codes
- [ ] Caregivers display with full information
- [ ] Copy code works
- [ ] Call button opens dialer
- [ ] Logs show expected messages

**If all checked**: Everything is working! ✨

**If any unchecked**: Check logs and refer to troubleshooting guide.
