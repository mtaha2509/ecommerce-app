package com.example.ecommerceapp.models;

public class Review {
    private String id;
    private String productId;
    private String userId;
    private String userName;
    private String comment;
    private float rating;
    private long timestamp;

    // Required empty constructor for Firestore
    public Review() {}

    public Review(String productId, String userId, String userName, String comment, float rating) {
        this.productId = productId;
        this.userId = userId;
        this.userName = userName;
        this.comment = comment;
        this.rating = rating;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 