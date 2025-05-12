package com.example.ecommerceapp.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class Order {
    private String id;
    private String userId;
    private String orderNumber;
    private @ServerTimestamp Date orderDate;
    private double totalAmount;
    private String status; // PENDING, PROCESSING, SHIPPED, COMPLETED, CANCELLED
    private List<OrderItem> items;
    private String shippingAddress;
    private String trackingNumber;
    private Date estimatedDeliveryDate;
    private List<String> sellerIds;
    private boolean redeemed; // Whether the amount has been redeemed by the seller
    
    // Required empty constructor for Firestore
    public Order() {
        this.redeemed = false; // Default to false
    }
    
    public Order(String userId, String orderNumber, double totalAmount, List<OrderItem> items, String shippingAddress) {
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = "PENDING";
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.redeemed = false;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public Date getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public Date getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }
    
    public void setEstimatedDeliveryDate(Date estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }
    
    public List<String> getSellerIds() {
        return sellerIds;
    }
    
    public void setSellerIds(List<String> sellerIds) {
        this.sellerIds = sellerIds;
    }
    
    public boolean isRedeemed() {
        return redeemed;
    }
    
    public void setRedeemed(boolean redeemed) {
        this.redeemed = redeemed;
    }
} 