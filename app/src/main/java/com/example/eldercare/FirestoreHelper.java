package com.example.eldercare;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreHelper {

    private final FirebaseFirestore db;

    public FirestoreHelper() {
        this.db = FirebaseFirestore.getInstance();
    }

    public String getCurrentUserId() {
        return FirebaseAuth.getInstance().getUid();
    }

    public CollectionReference getAppointmentsCollection() {
        String uid = getCurrentUserId();
        if (uid == null) return db.collection("appointments"); // Fallback
        return db.collection("users").document(uid).collection("appointments");
    }

    public CollectionReference getMedicationsCollection() {
        String uid = getCurrentUserId();
        if (uid == null) return db.collection("medications"); // Fallback
        return db.collection("users").document(uid).collection("medications");
    }

    public CollectionReference getVitalsCollection(String userId) {
        return db.collection("users").document(userId).collection("vitals");
    }
    
    public CollectionReference getHealthRecordsCollection(String userId) {
        return db.collection("users").document(userId).collection("healthRecords");
    }

    public CollectionReference getAlertsCollection(String userId) {
        return db.collection("users").document(userId).collection("alerts");
    }
}
