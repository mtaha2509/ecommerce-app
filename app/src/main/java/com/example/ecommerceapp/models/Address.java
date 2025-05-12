package com.example.ecommerceapp.models;

import java.util.UUID;

public class Address {
    private String id;
    private String name; // Name of the address (e.g., "Home", "Work")
    private String recipientName;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phoneNumber;
    private boolean isDefault;
    
    // Default constructor required for Firestore
    public Address() {
        this.id = UUID.randomUUID().toString();
    }
    
    public Address(String name, String recipientName, String street, String city, String state, 
                  String zipCode, String country, String phoneNumber) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.recipientName = recipientName;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.phoneNumber = phoneNumber;
        this.isDefault = false;
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
    
    public String getRecipientName() {
        return recipientName;
    }
    
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    
    public String getStreet() {
        return street;
    }
    
    public void setStreet(String street) {
        this.street = street;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    // Helper method to get formatted full address
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street).append(", ");
        sb.append(city).append(", ");
        sb.append(state).append(" ");
        sb.append(zipCode).append(", ");
        sb.append(country);
        return sb.toString();
    }
} 