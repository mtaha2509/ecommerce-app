package com.example.ecommerceapp.models;

public class OrderItem {
    private String productId;
    private String title;
    private double price;
    private int quantity;
    private String imageUrl;
    
    // Required empty constructor for Firestore
    public OrderItem() {
    }
    
    public OrderItem(String productId, String title, double price, int quantity, String imageUrl) {
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }
    
    // Getters and setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    // Calculate subtotal for this item
    public double getSubtotal() {
        return price * quantity;
    }
} 