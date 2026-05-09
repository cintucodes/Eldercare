package com.example.eldercare;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class AlertRecord {
    private String id;
    private String title;
    private String message;
    private String type; // health_sos, sedentary, etc.
    private String severity; // critical, warning, info
    private String elderId;
    private String createdBy;
    private Timestamp timestamp;
    private boolean dismissed;
    private boolean resolved;
    private boolean acknowledged; // Fix 3: Alert acknowledgement
    private Double gpsLat;
    private Double gpsLng;
    private String locationString;
    private String status;

    public AlertRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getElderId() { return elderId; }
    public void setElderId(String elderId) { this.elderId = elderId; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    @PropertyName("dismissed")
    public boolean isDismissed() { return dismissed; }
    @PropertyName("dismissed")
    public void setDismissed(boolean dismissed) { this.dismissed = dismissed; }
    
    @PropertyName("isResolved")
    public boolean isResolved() { return resolved; }
    @PropertyName("isResolved")
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    @PropertyName("isAcknowledged")
    public boolean isAcknowledged() { return acknowledged; }
    @PropertyName("isAcknowledged")
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    @PropertyName("latitude")
    public Double getGpsLat() { return gpsLat; }
    @PropertyName("latitude")
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    
    @PropertyName("longitude")
    public Double getGpsLng() { return gpsLng; }
    @PropertyName("longitude")
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }

    public String getLocationString() { return locationString; }
    public void setLocationString(String locationString) { this.locationString = locationString; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
