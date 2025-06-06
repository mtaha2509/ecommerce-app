package com.example.ecommerceapp.common.models;

import java.util.List;

public class Product {
    private String id;
    private String title;
    private String description;
    private String category;
    private double price;
    private List<String> imageUrls;
    private String glbUrl;  // URL for the 3D model
    private String videoUrl; // URL for the preview video
    private String userId;
    private long timestamp;

    // Required empty constructor for Firestore
    public Product() {}

    public Product(String title, String description, String category, double price, List<String> imageUrls, String glbUrl, String videoUrl, String userId) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.imageUrls = imageUrls;
        this.glbUrl = glbUrl;
        this.videoUrl = videoUrl;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getGlbUrl() { return glbUrl; }
    public void setGlbUrl(String glbUrl) { this.glbUrl = glbUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 