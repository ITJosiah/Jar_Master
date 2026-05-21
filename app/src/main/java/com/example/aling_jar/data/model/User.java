package com.example.aling_jar.data.model;

import com.google.firebase.Timestamp;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String mobile;
    private String address;
    private String role;
    private String photoUrl;
    private long createdAt;

    public User() {
        // Required for Firestore
    }

    public User(String uid, String fullName, String email, String mobile, String address, String role, String photoUrl, long createdAt) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.address = address;
        this.role = role;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
