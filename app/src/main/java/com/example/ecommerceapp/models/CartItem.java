package com.example.ecommerceapp.models;

public class CartItem {
    private String cartItemId;
    private String productId;
    private String userId;
    private String title;
    private double price;
    private String imageUrl;
    private int quantity;
    private long timestamp;
    private String sellerId;

    // Empty constructor required for Firestore
    public CartItem() {}

    public CartItem(String productId, String userId, String title, double price, String imageUrl, int quantity) {
        this.productId = productId;
        this.userId = userId;
        this.title = title;
        this.price = price;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor with sellerId
    public CartItem(String productId, String userId, String sellerId, String title, double price, String imageUrl, int quantity) {
        this.productId = productId;
        this.userId = userId;
        this.sellerId = sellerId;
        this.title = title;
        this.price = price;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(String cartItemId) {
        this.cartItemId = cartItemId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
} 