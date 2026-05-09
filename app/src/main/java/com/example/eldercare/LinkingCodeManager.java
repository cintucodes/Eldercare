package com.example.eldercare;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LinkingCodeManager {

    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public LinkingCodeManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void generateCode(String elderId, Callback<String> callback) {
        android.util.Log.d("LinkingCodeManager", "generateCode called for elderId: " + elderId);
        
        String code = generateRandomNumeric(6);
        android.util.Log.d("LinkingCodeManager", "Generated code: " + code);
        
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.HOUR, 48);
        Date expiresAt = cal.getTime();

        Map<String, Object> linkingCode = new HashMap<>();
        linkingCode.put("elderId", elderId);
        linkingCode.put("createdAt", new Timestamp(now));
        linkingCode.put("expiresAt", new Timestamp(expiresAt));
        linkingCode.put("used", false);
        linkingCode.put("usedBy", null);
        
        android.util.Log.d("LinkingCodeManager", "Saving code to Firestore: linkingCodes/" + code);

        db.collection("linkingCodes").document(code)
                .set(linkingCode)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("LinkingCodeManager", "Code saved successfully to Firestore");
                    callback.onSuccess(code);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LinkingCodeManager", "Failed to save code to Firestore: " + e.getMessage(), e);
                    callback.onFailure(e);
                });
    }

    /**
     * Claims a code and returns the linked elderId via the callback.
     */
    public void claimCode(String enteredCode, String caregiverId, Callback<String> callback) {
        DocumentReference codeRef = db.collection("linkingCodes").document(enteredCode);

        codeRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Boolean used = document.getBoolean("used");
                    Timestamp expiresAt = document.getTimestamp("expiresAt");
                    String elderId = document.getString("elderId");

                    if (Boolean.TRUE.equals(used)) {
                        callback.onFailure(new Exception("Code already used"));
                        return;
                    }

                    if (expiresAt != null && expiresAt.toDate().before(new Date())) {
                        callback.onFailure(new Exception("Code expired"));
                        return;
                    }

                    if (elderId == null) {
                        callback.onFailure(new Exception("Invalid code data"));
                        return;
                    }

                    executeClaimBatch(enteredCode, caregiverId, elderId, callback);
                } else {
                    callback.onFailure(new Exception("Invalid code"));
                }
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    private void executeClaimBatch(String code, String caregiverId, String elderId, Callback<String> callback) {
        db.collection("users").document(caregiverId).get()
                .addOnSuccessListener(cgDoc -> {
                    WriteBatch batch = db.batch();

                    DocumentReference codeRef = db.collection("linkingCodes").document(code);
                    batch.update(codeRef, "used", true, "usedBy", caregiverId);

                    DocumentReference caregiverRef = db.collection("users").document(caregiverId);
                    batch.update(caregiverRef, "linkedElderId", elderId);

                    DocumentReference elderRef = db.collection("users").document(elderId);
                    batch.update(elderRef, "linkedCaregivers", FieldValue.arrayUnion(caregiverId));

                    DocumentReference caregiverSubDocRef = db.collection("users").document(elderId)
                            .collection("caregivers").document(caregiverId);
                    Map<String, Object> caregiverData = new HashMap<>();
                    caregiverData.put("linkedAt", FieldValue.serverTimestamp());
                    
                    if (cgDoc.exists()) {
                        String firstName = cgDoc.getString("firstName");
                        String lastName = cgDoc.getString("lastName");
                        String name = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                        caregiverData.put("name", name.trim());
                        
                        if (cgDoc.contains("phone")) {
                            caregiverData.put("phone", cgDoc.getString("phone"));
                        }
                        if (cgDoc.contains("email")) {
                            caregiverData.put("email", cgDoc.getString("email"));
                        }
                        if (cgDoc.contains("relationship")) {
                            caregiverData.put("relationship", cgDoc.getString("relationship"));
                        } else {
                            caregiverData.put("relationship", "Caregiver");
                        }
                    }
                    
                    batch.set(caregiverSubDocRef, caregiverData);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(elderId))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private String generateRandomNumeric(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
