# Fix: Blood Pressure Showing as Generic "Activity" Entry

## Problem
Blood pressure data was showing as generic "Activity: ..." in the "Recent Vitals Updates" section instead of displaying properly as "Blood Pressure: 120/80 mmHg".

## Root Cause
The mock Health Connect data was saving blood pressure as TWO separate entries:
- `systolicBP` with single value
- `diastolicBP` with single value

But the ActivityFeedAdapter expects blood pressure as ONE combined entry:
- `blood_pressure` type with BOTH `systolic` and `diastolic` fields

## Solution

### Added Combined Blood Pressure Entry in ElderProfileFragment.java

```java
// NEW: Save combined blood pressure entry for activity feed
saveCombinedBloodPressure(db, systolic, diastolic, now);

private void saveCombinedBloodPressure(FirebaseFirestore db, int systolic, int diastolic, Timestamp timestamp) {
    Map<String, Object> bpRecord = new HashMap<>();
    bpRecord.put("elderId", uid);
    bpRecord.put("type", "blood_pressure");  // ← Correct type for activity feed
    bpRecord.put("systolic", systolic);      // ← Both values in one document
    bpRecord.put("diastolic", diastolic);
    bpRecord.put("timestamp", timestamp);
    bpRecord.put("source", "Health Connect");
    
    db.collection("users").document(uid).collection("vitals").add(bpRecord);
}
```

## Data Structure Now

### For Activity Feed Display:
```
type: "blood_pressure"
systolic: 120
diastolic: 80
timestamp: ...
```
→ Shows as: "Blood Pressure: 120/80 mmHg" ✅

### For Individual Tracking (Home Tab):
```
type: "systolicBP"
value: 120
```
```
type: "diastolicBP"  
value: 80
```
→ Used by Home tab for real-time display ✅

## Result
✅ Blood pressure now displays correctly in "Recent Vitals Updates"
✅ Shows as "Blood Pressure: 120/80 mmHg" with blood drop icon
✅ Color-coded status dot (green/yellow/red) based on systolic value
✅ No more generic "Activity" entries

## Files Modified
1. ElderProfileFragment.java - Added saveCombinedBloodPressure() method
2. ActivityFeedAdapter.java - Already had correct handler for "blood_pressure" type
