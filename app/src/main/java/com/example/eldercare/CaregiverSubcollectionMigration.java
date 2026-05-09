package com.example.eldercare;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to migrate existing caregiver subcollections to include full caregiver details.
 * This is needed for caregivers linked before the fix was applied.
 * 
 * Usage: Call updateCaregiverSubcollections(elderId) after elder logs in
 */
public class CaregiverSubcollectionMigration {
    
    private static final String TAG = "CaregiverMigration";
    private final FirebaseFirestore db;
    
    public interface MigrationCallback {
        void onComplete(int updated, int failed);
    }
    
    public CaregiverSubcollectionMigration() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Updates all caregiver subcollections for an elder to include full caregiver details
     */
    public void updateCaregiverSubcollections(String elderId, MigrationCallback callback) {
        Log.d(TAG, "Starting migration for elder: " + elderId);
        
        // Get elder's linkedCaregivers array
        db.collection("users").document(elderId).get()
                .addOnSuccessListener(elderDoc -> {
                    if (!elderDoc.exists()) {
                        Log.w(TAG, "Elder document not found");
                        callback.onComplete(0, 0);
                        return;
                    }
                    
                    List<String> linkedCaregivers = (List<String>) elderDoc.get("linkedCaregivers");
                    if (linkedCaregivers == null || linkedCaregivers.isEmpty()) {
                        Log.d(TAG, "No linked caregivers found");
                        callback.onComplete(0, 0);
                        return;
                    }
                    
                    Log.d(TAG, "Found " + linkedCaregivers.size() + " linked caregivers");
                    
                    final int[] updated = {0};
                    final int[] failed = {0};
                    final int[] processed = {0};
                    int total = linkedCaregivers.size();
                    
                    for (String caregiverId : linkedCaregivers) {
                        updateSingleCaregiver(elderId, caregiverId, success -> {
                            if (success) {
                                updated[0]++;
                            } else {
                                failed[0]++;
                            }
                            processed[0]++;
                            
                            if (processed[0] == total) {
                                Log.d(TAG, "Migration complete: " + updated[0] + " updated, " + failed[0] + " failed");
                                callback.onComplete(updated[0], failed[0]);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch elder document: " + e.getMessage());
                    callback.onComplete(0, 1);
                });
    }
    
    private void updateSingleCaregiver(String elderId, String caregiverId, SingleUpdateCallback callback) {
        Log.d(TAG, "Updating caregiver: " + caregiverId);
        
        // Check if subcollection document already has all fields
        db.collection("users").document(elderId)
                .collection("caregivers").document(caregiverId)
                .get()
                .addOnSuccessListener(subDoc -> {
                    if (subDoc.exists() && subDoc.contains("name") && subDoc.contains("phone")) {
                        Log.d(TAG, "Caregiver " + caregiverId + " already has full data, skipping");
                        callback.onComplete(true);
                        return;
                    }
                    
                    // Fetch caregiver details from users collection
                    db.collection("users").document(caregiverId).get()
                            .addOnSuccessListener(cgDoc -> {
                                if (!cgDoc.exists()) {
                                    Log.w(TAG, "Caregiver document not found: " + caregiverId);
                                    callback.onComplete(false);
                                    return;
                                }
                                
                                Map<String, Object> updates = new HashMap<>();
                                
                                String firstName = cgDoc.getString("firstName");
                                String lastName = cgDoc.getString("lastName");
                                String name = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                                updates.put("name", name.trim());
                                
                                if (cgDoc.contains("phone")) {
                                    updates.put("phone", cgDoc.getString("phone"));
                                }
                                if (cgDoc.contains("email")) {
                                    updates.put("email", cgDoc.getString("email"));
                                }
                                if (cgDoc.contains("relationship")) {
                                    updates.put("relationship", cgDoc.getString("relationship"));
                                } else {
                                    updates.put("relationship", "Caregiver");
                                }
                                
                                // Preserve existing linkedAt if present
                                if (!subDoc.exists() || !subDoc.contains("linkedAt")) {
                                    updates.put("linkedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                }
                                
                                Log.d(TAG, "Updating subcollection for " + caregiverId + " with: " + updates);
                                
                                // Update subcollection document
                                db.collection("users").document(elderId)
                                        .collection("caregivers").document(caregiverId)
                                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Successfully updated caregiver: " + caregiverId);
                                            callback.onComplete(true);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to update caregiver " + caregiverId + ": " + e.getMessage());
                                            callback.onComplete(false);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to fetch caregiver details: " + e.getMessage());
                                callback.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check subcollection: " + e.getMessage());
                    callback.onComplete(false);
                });
    }
    
    private interface SingleUpdateCallback {
        void onComplete(boolean success);
    }
}
