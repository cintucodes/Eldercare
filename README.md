# ElderCare - Remote Health Monitoring for Seniors

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📱 Overview

ElderCare is a comprehensive Android application designed to help caregivers remotely monitor the health and safety of elderly family members. The app provides real-time health tracking, medication reminders, emergency SOS alerts, and seamless communication between elders and their caregivers.

## ✨ Key Features

### For Elders
- **📊 Health Vitals Tracking**: Log heart rate, blood pressure, glucose, sleep, and steps
- **🔗 Health Connect Integration**: Automatic sync from wearable devices (Fitbit, Samsung Watch, Garmin, etc.)
- **💊 Medication Management**: Set reminders with alarm notifications
- **📅 Appointment Scheduling**: Track doctor visits and medical appointments
- **🆘 Emergency SOS**: One-tap emergency alert with GPS location sent to caregivers via SMS
- **👤 Profile Management**: Store medical history, allergies, and chronic conditions
- **🎯 Customizable Step Goals**: Set personalized daily activity targets

### For Caregivers
- **📈 Real-Time Monitoring**: View elder's health data instantly via Firebase sync
- **🚨 Automated Alerts**: Receive push notifications for abnormal vitals
  - Heart rate: <60 or >100 bpm (RED alert)
  - Sleep: <5 hours (AMBER alert)
  - Emergency SOS alerts
- **📱 Remote Dashboard**: Monitor multiple vitals at a glance
- **🔗 Easy Linking**: Connect to elders via secure invite codes
- **📞 Quick Actions**: Call or view details directly from notifications
- **📊 Activity Feed**: Track elder's daily health activities

## 🏗️ Architecture

### Technology Stack
- **Language**: Java, Kotlin
- **Backend**: Firebase (Firestore, Authentication, Cloud Messaging)
- **Health Integration**: Android Health Connect SDK
- **Architecture Pattern**: MVVM with Repository pattern
- **Background Processing**: WorkManager, Foreground Services
- **Notifications**: NotificationCompat with channels and grouping

### Key Components
- **RuleEngine**: Analyzes vitals and generates automated alerts
- **SOSManager**: Handles emergency alerts with location and SMS
- **HealthConnectBridge**: Interfaces with wearable devices
- **CaregiverAlertService**: Foreground service for real-time monitoring
- **MedicationReminderScheduler**: Manages medication alarm scheduling

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 24+ (Android 7.0+)
- Firebase account
- Google Services JSON configuration file

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/eldercare.git
   cd eldercare
   ```

2. **Set up Firebase** (REQUIRED)
   
   a. Create a new Firebase project:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Add project" and follow the setup wizard
   - Choose a project name (e.g., "ElderCare-Production")
   
   b. Add Android app to Firebase:
   - Click "Add app" → Select Android
   - Package name: `com.example.eldercare`
   - Download `google-services.json`
   - Place it in `app/` directory (see `app/google-services.json.example` for reference)
   
   c. Enable Firebase services:
   - **Authentication**: 
     - Go to Authentication → Sign-in method
     - Enable "Email/Password" provider
   - **Firestore Database**:
     - Go to Firestore Database → Create database
     - Start in "test mode" (we'll add security rules next)
   - **Cloud Messaging**:
     - Already enabled by default
     - Note: FCM is used for caregiver alerts
   
   d. Deploy Firestore Security Rules:
   ```bash
   firebase deploy --only firestore:rules
   ```
   Or manually copy rules from `firestore.rules` to Firebase Console → Firestore → Rules tab

3. **Configure Android Studio**
   - Open project in Android Studio
   - Wait for Gradle sync to complete
   - Verify `google-services.json` is in `app/` directory
   - Build → Clean Project
   - Build → Rebuild Project

4. **Run the App**
   - Connect Android device (API 24+) or start emulator
   - Run → Run 'app'
   - Create test accounts (see Testing section below)

### Testing Without Wearable Devices

The app includes **mock data generators** for testing without physical wearables:

1. **Health Connect Demo Mode**:
   - Go to Profile tab → "Sync from Health Connect"
   - Choose test scenario: Normal Data, Full Day, or Abnormal
   - Mock vitals will be generated and synced to Firebase
   - This simulates real Health Connect integration

2. **Why Mock Data?**:
   - Health Connect requires physical wearable devices (Fitbit, Samsung Watch, etc.)
   - Mock data allows testing alerts, caregiver notifications, and UI
   - Clearly labeled as "Test Health Connect Sync" in UI

### Firebase Configuration

**Firestore Collections Structure:**
```
users/
  {userId}/
    - role: "elder" | "caregiver"
    - firstName, lastName, email, phone
    - stepGoal: number (default: 5000)
    - linkedCaregivers: array of caregiver UIDs
    
    vitals/
      {vitalId}/
        - type: "heartRate" | "steps" | "bloodGlucose" | "sleepHours"
        - value: number
        - timestamp: timestamp
        - source: "manual" | "Health Connect"
    
    medications/
      {medId}/
        - name, dosage, frequency
        - reminderTimes: array
        - isActive: boolean
    
    alerts/
      {alertId}/
        - title, message, type
        - severity: "RED" | "AMBER" | "GREEN"
        - timestamp, isResolved, isAcknowledged
```

## 📸 Screenshots

*(Add screenshots of your app here)*

- Elder Dashboard
- Vitals Tracking
- Medication Reminders
- Caregiver Monitoring
- SOS Alert Flow

## 🔐 Security & Privacy

- **Authentication**: Firebase Authentication with email/password
- **Data Security**: Firestore security rules enforce role-based access
- **Encryption**: All data transmitted over HTTPS
- **Privacy**: Health data only shared with linked caregivers
- **Permissions**: Runtime permissions for SMS, location, notifications

## 🧪 Testing

### Quick Start Testing Guide

1. **Create Test Accounts**:
   - Launch app → Register → Create Elder account
   - Note the email/password
   - Logout → Register → Create Caregiver account

2. **Link Elder and Caregiver**:
   - Login as Elder → Home tab → "Link Caregiver" button
   - Copy the 6-digit code
   - Login as Caregiver → Enter elder's code
   - Verify connection successful

3. **Test Health Vitals**:
   - Login as Elder → Health tab
   - **Option A**: Manually log vitals (heart rate, BP, glucose, etc.)
   - **Option B**: Profile tab → "Sync from Health Connect" → Choose test scenario
   - Verify vitals appear on Elder's Home dashboard
   - Login as Caregiver → Verify vitals visible in Overview

4. **Test Automated Alerts**:
   - Login as Elder → Log abnormal vitals:
     - Heart rate: 105 bpm (triggers RED alert)
     - Sleep: 4 hours (triggers AMBER alert)
   - Login as Caregiver → Check Alerts tab
   - Verify push notification received

5. **Test SOS Emergency**:
   - Login as Elder → Home tab → Red SOS button
   - Grant location and SMS permissions when prompted
   - Confirm 3-second countdown
   - Verify alert created with location
   - Login as Caregiver → Check Alerts tab for SOS
   - Verify SMS sent to caregiver's phone (if configured)

6. **Test Medications**:
   - Login as Elder → Medications tab → Add medication
   - Set reminder time (e.g., 2 minutes from now)
   - Wait for alarm notification
   - Verify full-screen alarm appears
   - Mark as taken
   - Login as Caregiver → Verify medication visible (view-only)

7. **Test Appointments**:
   - Login as Elder → Appointments tab → Add appointment
   - Set reminder time
   - Verify persistent notification appears
   - Tap phone number to call doctor
   - Mark as attended
   - Login as Caregiver → Verify attendance status visible

### Test Data Scenarios

Use "Sync from Health Connect" with these scenarios:

- **Normal Data**: Healthy vitals (HR: 70-80, Steps: 7000-10000, Sleep: 7-8h)
- **Full Day**: Realistic day pattern with morning/midday/evening readings
- **Abnormal**: Triggers alerts (HR: 105, Sleep: 4h, Glucose: 180)

### Troubleshooting

- **No vitals showing**: Check Firebase console → Firestore → users/{uid}/vitals
- **Alerts not received**: Verify FCM setup and caregiver is linked
- **SOS not working**: Grant location and SMS permissions in Android settings
- **Medication alarm not ringing**: Enable "Alarms & reminders" permission (Android 12+)

## 🐛 Known Issues & Limitations

- **Health Connect**: Requires physical wearable device for real production sync (use mock data for testing)
- **SMS Testing**: In demo environment, SMS may loop back to same device if elder and caregiver use same phone
- **Sedentary Monitoring**: Temporarily disabled to prevent false positives during development
- **FCM Notifications**: Require Firebase Cloud Messaging setup (included in google-services.json)
- **Location Services**: SOS requires GPS/Network location enabled on device

## 🛣️ Roadmap

- [ ] Wear OS companion app for wearable emergency button
- [ ] Multi-language support
- [ ] Data export (PDF reports for doctors)
- [ ] Video call integration
- [ ] AI-powered health insights
- [ ] Medication interaction warnings
- [ ] Fall detection via accelerometer

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate comments.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Your Name** - *Initial work* - [YourGitHub](https://github.com/yourusername)

## 🙏 Acknowledgments

- Firebase for backend infrastructure
- Android Health Connect team
- Material Design components
- Open source community

## 📞 Support

For questions or issues:
- Open an issue on GitHub
- Email: your.email@example.com

## 📊 Project Status

**Current Version**: 1.0.0 (Beta)  
**Status**: Active Development  
**Last Updated**: January 2025

---

**Note**: This app is designed for demonstration and educational purposes. For production medical use, ensure compliance with healthcare regulations (HIPAA, GDPR, etc.) and consult with medical professionals.
