package com.example.eldercare;

import java.util.List;

public class PatientRecord {
    private String id;
    private String fullName;
    private int age;
    private String photoUrl;
    private String status; // NORMAL, WARNING, CRITICAL
    private List<String> conditions;
    private String phone;

    public PatientRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getConditions() { return conditions; }
    public void setConditions(List<String> conditions) { this.conditions = conditions; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
