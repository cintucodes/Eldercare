# Firebase Setup Guide for ElderCare

This guide will help you set up Firebase for the ElderCare app.

## Prerequisites

- Google account
- Android Studio installed
- ElderCare project cloned

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Enter project name: `ElderCare` (or your preferred name)
4. Disable Google Analytics (optional for development)
5. Click **"Create project"**

## Step 2: Add Android App to Firebase

1. In Firebase Console, click **"Add app"** → Select **Android**
2. Enter package name: `com.example.eldercare`
   - Find this in `app/build.gradle.kts` under `applicationId`
3. Enter app nickname: `ElderCare Android`
4. Leave SHA-1 blank for now (needed later for advanced features)
5. Click **"Register app"**

## Step 3: Download Configuration File

1. Download `google-services.json`
2. Place it in `app/` directory (same level as `build.gradle.kts`)
3. **IMPORTANT**: Verify `google-services.json` is in `.gitignore` (it should be)

## Step 4: Enable Firebase Services

### Authentication

1. In Firebase Console, go to **Authentication** → **Get started**
2. Click **Sign-in method** tab
3. Enable **Email/Password**
4. Click **Save**

### Firestore Database

1. Go to **Firestore Database** → **Create database**
2. Select **Start in production mode**
3. Choose a location (closest to your users)
4. Click **Enable**

### Deploy Security Rules

1. In Firebase Console, go to **Firestore Database** → **Rules** tab
2. Copy the contents of `firestore.rules` from the project
3. Paste into the Firebase Console rules editor
4. Click **Publish**

**Or use Firebase CLI:**
```bash
npm install -g firebase-tools
firebase login
firebase init firestore
firebase deploy --only firestore:rules
```

### Cloud Messaging (Optional but Recommended)

1. Go to **Cloud Messaging** → **Get started**
2. No additional configuration needed
3. Notifications will work automatically

## Step 5: Verify Setup

1. Open project in Android Studio
2. Sync Gradle files
3. Build the project
4. Run on emulator or device
5. Try registering a new user
6. Check Firebase Console → Authentication to see the new user

## Firestore Collections Structure

The app will automatically create these collections:

```
users/
  {userId}/
    - Fields: role, firstName, lastName, email, phone, stepGoal
    
    vitals/ (subcollection)
      - Individual vital records
      
    medications/ (subcollection)
      - Medication records
      
    appointments/ (subcollection)
      - Appointment records
      
    alerts/ (subcollection)
      - Health alerts
      
    caregivers/ (subcollection)
      - Linked caregiver info

linkingCodes/
  {code}/
    - Fields: elderUid, createdAt, expiresAt, claimed
```

## Security Rules Overview

The deployed rules ensure:
- Users can only read/write their own data
- Caregivers can read linked elder's data
- Elders can update their `linkedCaregivers` field
- Linking codes have expiration logic

## Troubleshooting

### "google-services.json not found"
- Ensure file is in `app/` directory
- Sync Gradle files
- Clean and rebuild project

### "Authentication failed"
- Verify Email/Password is enabled in Firebase Console
- Check internet connection
- Verify `google-services.json` package name matches `applicationId`

### "Permission denied" in Firestore
- Verify security rules are deployed
- Check user is authenticated
- Verify user role is set correctly

### "Notifications not working"
- Ensure Cloud Messaging is enabled
- Check notification permissions are granted
- Verify device has internet connection

## Testing with Multiple Users

1. **Create Elder Account:**
   - Register with role "Elder"
   - Note the user ID from Firebase Console

2. **Create Caregiver Account:**
   - Register with role "Caregiver"
   - Use elder's invite code to link

3. **Test Features:**
   - Log vitals as elder
   - Check caregiver receives notifications
   - Test SOS alert
   - Add medications and appointments

## Production Considerations

Before deploying to production:

1. **Enable App Check** (prevents abuse)
2. **Set up proper indexes** for complex queries
3. **Configure billing alerts**
4. **Review security rules** thoroughly
5. **Enable crash reporting** (Firebase Crashlytics)
6. **Set up backup strategy** for Firestore data

## Cost Estimation

Firebase free tier includes:
- 50,000 reads/day
- 20,000 writes/day
- 1 GB storage
- 10 GB/month network egress

For a small user base (<100 users), free tier should be sufficient.

## Support

If you encounter issues:
1. Check [Firebase Documentation](https://firebase.google.com/docs)
2. Search [Stack Overflow](https://stackoverflow.com/questions/tagged/firebase)
3. Open an issue on GitHub

## Next Steps

After Firebase setup:
- Configure Health Connect (see README.md)
- Test all features
- Deploy to test devices
- Gather user feedback

---

**Last Updated**: January 2025
