package com.example.ecommerceapp.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class ReleaseTransaction {
    private String id;
    private String sellerId;
    private double amount;
    private @ServerTimestamp Date releaseDate;
    private List<String> orderIds; // IDs of orders included in this release
    
    // Empty constructor required for Firestore
    public ReleaseTransaction() {
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public Date getReleaseDate() {
        return releaseDate;
    }
    
    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public List<String> getOrderIds() {
        return orderIds;
    }
    
    public void setOrderIds(List<String> orderIds) {
        this.orderIds = orderIds;
    }
} 