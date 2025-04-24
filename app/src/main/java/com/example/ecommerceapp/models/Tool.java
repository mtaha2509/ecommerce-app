package com.example.ecommerceapp.models;

public class Tool {
    private String name;
    private int iconResId;

    public Tool(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
} 