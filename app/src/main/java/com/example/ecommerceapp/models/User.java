package com.example.ecommerceapp.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role; // buyer or seller
    private String imageUrl;
    private String bio;
    
    // For sellers
    private String businessName;
    private String businessDescription;
    
    // For shipping
    private List<Address> addresses;
    
    // Default constructor required for Firestore
    public User() {
        addresses = new ArrayList<>();
    }
    
    public User(String id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        addresses = new ArrayList<>();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getBusinessName() {
        return businessName;
    }
    
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    
    public String getBusinessDescription() {
        return businessDescription;
    }
    
    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }
    
    public List<Address> getAddresses() {
        return addresses;
    }
    
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }
    
    // Helper methods
    public void addAddress(Address address) {
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        addresses.add(address);
    }
    
    public void removeAddress(Address address) {
        if (addresses != null) {
            addresses.remove(address);
        }
    }
    
    @Exclude
    public boolean isSeller() {
        return "seller".equalsIgnoreCase(role);
    }
    
    @Exclude
    public boolean isBuyer() {
        return "buyer".equalsIgnoreCase(role);
    }
} 