# Fix: Remove Generic "Activity" Entries from Recent Vitals Updates

## Problem
The "Recent Vitals Updates" section in caregiver dashboard was showing generic "Activity: ..." entries that shouldn't appear.

## Root Cause
- VitalsActivity saves vitals data WITHOUT a "type" field
- Health Connect sync saves vitals WITH specific type fields (heartRate, bloodGlucose, steps, etc.)
- The activity feed was loading ALL vitals documents, including those without proper type fields
- Documents without type were falling through to the default case and showing as "Activity: ..."

## Solution

### 1. Stricter Filtering in CaregiverOverviewFragment.java
```java
// OLD: Only checked if type exists and isn't vitals_entry
if (type != null && !type.equals("vitals_entry") && !doc.getId().startsWith("latest_"))

// NEW: Also checks that type is not empty
if (type != null && !type.isEmpty() 
        && !type.equals("vitals_entry")
        && !docId.startsWith("latest_"))
```

### 2. Hide Unknown Types in ActivityFeedAdapter.java
```java
// Reset visibility at start of binding
holder.itemView.setVisibility(View.VISIBLE);

// If no type field, hide the item completely
if (type == null) {
    holder.itemView.setVisibility(View.GONE);
    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
    return;
}

// In default case, hide instead of showing generic "Activity"
default:
    holder.itemView.setVisibility(View.GONE);
    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
    return;
```

## Result
✅ Only valid Health Connect vitals entries show in "Recent Vitals Updates"
✅ Manual vitals entries (from VitalsActivity) are excluded since they don't have type field
✅ No more generic "Activity: ..." entries appear

## Valid Entry Types Shown
- heartRate / heart_rate
- blood_pressure
- bloodGlucose / glucose
- steps
- sleepHours

## Files Modified
1. CaregiverOverviewFragment.java - Stricter filtering in loadRecentActivity()
2. ActivityFeedAdapter.java - Hide items with null or unknown type
