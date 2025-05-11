package com.example.ecommerceapp.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Conversation {
    private String id;
    private String buyerId;
    private String sellerId;
    private String buyerName;
    private String sellerName;
    private String lastMessage;
    private @ServerTimestamp Date lastMessageTimestamp;
    private boolean unreadBuyer;    // True if buyer has unread messages
    private boolean unreadSeller;   // True if seller has unread messages
    private String productId;       // Optional - related product
    private String productTitle;    // Optional - related product title
    
    // Required empty constructor for Firestore
    public Conversation() {
    }
    
    public Conversation(String buyerId, String sellerId, String buyerName, String sellerName, String productId, String productTitle) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.lastMessage = "";
        this.unreadBuyer = false;
        this.unreadSeller = false;
        this.productId = productId;
        this.productTitle = productTitle;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getBuyerId() {
        return buyerId;
    }
    
    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getBuyerName() {
        return buyerName;
    }
    
    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }
    
    public String getSellerName() {
        return sellerName;
    }
    
    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }
    
    public void setLastMessageTimestamp(Date lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
    
    public boolean isUnreadBuyer() {
        return unreadBuyer;
    }
    
    public void setUnreadBuyer(boolean unreadBuyer) {
        this.unreadBuyer = unreadBuyer;
    }
    
    public boolean isUnreadSeller() {
        return unreadSeller;
    }
    
    public void setUnreadSeller(boolean unreadSeller) {
        this.unreadSeller = unreadSeller;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductTitle() {
        return productTitle;
    }
    
    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }
} 