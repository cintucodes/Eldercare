# New Features Added - Alerts Management & CSV Download

## 1. Clear All Resolved Alerts Feature

### Changes Made:
- **fragment_alerts.xml**: Added "Clear All Resolved" button at top of alerts list
- **AlertsFragment.java**: 
  - Added `allAlerts` list to track all alerts
  - Added `setupClearButton()` method
  - Added `updateClearButtonVisibility()` - button only shows when resolved alerts exist
  - Added `confirmClearResolved()` - shows confirmation dialog with count
  - Added `clearResolvedAlerts()` - bulk deletes all resolved alerts from Firestore

### How It Works:
1. Button appears at top-right only when there are resolved alerts
2. Tap button → shows dialog: "Delete all X resolved alerts?"
3. Confirm → permanently deletes all resolved alerts from database
4. Button auto-hides when no resolved alerts remain

### User Workflow:
- **New Alert** → Acknowledge → Mark Resolved → Clear All Resolved (bulk delete)
- **Long-press** any alert → Delete individual alert (still works)

---

## 2. CSV Download to Files Feature

### Changes Made:
- **activity_reports.xml**: 
  - Renamed "Export CSV" button to "Share CSV" (clarifies it opens share dialog)
  - Added new "Download CSV to Files" button with download icon
- **ReportsActivity.java**:
  - Added `btnDownloadCSV` button reference
  - Added `downloadCSV()` method that saves to public Downloads folder
  - Existing `exportToCSV()` still opens share dialog
- **baseline_download_24.xml**: Created download icon

### How It Works:

**Share CSV Button** (existing):
- Creates CSV file in app's private Documents/Reports folder
- Opens Android share dialog
- User can share via email, WhatsApp, Drive, etc.

**Download CSV to Files Button** (NEW):
- Creates CSV file in public Downloads folder
- File is visible in Files app immediately
- Shows toast: "Downloaded to: HealthReport_[timestamp].csv"
- User can find it in Files app → Downloads folder

### CSV Format (both methods):
```
Date,Heart Rate,Systolic BP,Diastolic BP,Steps
2024-01-15 08:30,72,120,80,5000
2024-01-15 14:45,75,118,78,8500
```

---

## Files Modified:
1. `fragment_alerts.xml` - Added clear button
2. `AlertsFragment.java` - Added bulk delete logic
3. `activity_reports.xml` - Added download button, renamed share button
4. `ReportsActivity.java` - Added download method
5. `baseline_download_24.xml` - Created download icon

---

## Testing Checklist:
- [ ] Alerts: Clear All Resolved button appears only when resolved alerts exist
- [ ] Alerts: Bulk delete confirmation shows correct count
- [ ] Alerts: All resolved alerts deleted successfully
- [ ] Reports: "Share CSV" opens share dialog
- [ ] Reports: "Download CSV to Files" saves to Downloads folder
- [ ] Reports: Downloaded CSV visible in Files app
- [ ] Reports: Both CSV files have correct format and data
