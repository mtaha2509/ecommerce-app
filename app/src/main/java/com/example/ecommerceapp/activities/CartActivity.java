package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.CartAdapter;
import com.example.ecommerceapp.models.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartItemListener {
    private static final String TAG = "CartActivity";
    
    private RecyclerView recyclerView;
    private TextView tvEmptyCart, tvTotalAmount;
    private Button btnCheckout, btnContinueShopping;
    
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String userId;
    
    private double totalAmount = 0.0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view cart", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        userId = currentUser.getUid();
        
        initializeViews();
        setupRecyclerView();
        loadCartItems();
        
        btnCheckout.setOnClickListener(v -> checkout());
        btnContinueShopping.setOnClickListener(v -> finish());
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewCart);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnContinueShopping = findViewById(R.id.btnContinueShopping);
    }
    
    private void setupRecyclerView() {
        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartItems, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(cartAdapter);
    }
    
    private void loadCartItems() {
        firestore.collection("cart")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartItems.clear();
                        totalAmount = 0.0;
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CartItem item = document.toObject(CartItem.class);
                            item.setCartItemId(document.getId());
                            cartItems.add(item);
                            
                            // Calculate total amount
                            totalAmount += (item.getPrice() * item.getQuantity());
                        }
                        
                        updateUI();
                        cartAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error getting cart items", task.getException());
                        Toast.makeText(CartActivity.this, "Error loading cart: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void updateUI() {
        if (cartItems.isEmpty()) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnCheckout.setEnabled(false);
        } else {
            tvEmptyCart.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnCheckout.setEnabled(true);
        }
        
        tvTotalAmount.setText(String.format("Total: $%.2f", totalAmount));
    }
    
    @Override
    public void onQuantityChange(CartItem item, int newQuantity) {
        if (newQuantity <= 0) {
            // Remove item if quantity is zero or negative
            new AlertDialog.Builder(this)
                    .setTitle("Remove Item")
                    .setMessage("Do you want to remove this item from your cart?")
                    .setPositiveButton("Yes", (dialog, which) -> removeCartItem(item))
                    .setNegativeButton("No", (dialog, which) -> {
                        // Keep at least 1 quantity
                        updateCartItemQuantity(item, 1);
                    })
                    .show();
        } else {
            updateCartItemQuantity(item, newQuantity);
        }
    }
    
    @Override
    public void onItemRemove(CartItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Item")
                .setMessage("Do you want to remove this item from your cart?")
                .setPositiveButton("Yes", (dialog, which) -> removeCartItem(item))
                .setNegativeButton("No", null)
                .show();
    }
    
    private void removeCartItem(CartItem item) {
        firestore.collection("cart").document(item.getCartItemId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    cartItems.remove(item);
                    totalAmount -= (item.getPrice() * item.getQuantity());
                    updateUI();
                    cartAdapter.notifyDataSetChanged();
                    Toast.makeText(CartActivity.this, "Item removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing item", e);
                    Toast.makeText(CartActivity.this, "Error removing item: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateCartItemQuantity(CartItem item, int newQuantity) {
        firestore.collection("cart").document(item.getCartItemId())
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    // Recalculate total
                    totalAmount -= (item.getPrice() * item.getQuantity()); // Subtract old amount
                    item.setQuantity(newQuantity);
                    totalAmount += (item.getPrice() * item.getQuantity()); // Add new amount
                    
                    tvTotalAmount.setText(String.format("Total: $%.2f", totalAmount));
                    cartAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating quantity", e);
                    Toast.makeText(CartActivity.this, "Error updating quantity: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create order document
        Map<String, Object> order = new HashMap<>();
        order.put("userId", userId);
        order.put("orderDate", System.currentTimeMillis());
        order.put("totalAmount", totalAmount);
        order.put("status", "Pending");
        
        firestore.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();
                    
                    // Add order items
                    List<Map<String, Object>> orderItems = new ArrayList<>();
                    for (CartItem item : cartItems) {
                        Map<String, Object> orderItem = new HashMap<>();
                        orderItem.put("productId", item.getProductId());
                        orderItem.put("title", item.getTitle());
                        orderItem.put("price", item.getPrice());
                        orderItem.put("quantity", item.getQuantity());
                        orderItem.put("imageUrl", item.getImageUrl());
                        orderItems.add(orderItem);
                    }
                    
                    documentReference.update("items", orderItems)
                            .addOnSuccessListener(aVoid -> {
                                // Clear cart after successful order
                                clearCart();
                                
                                // Show success and navigate to order confirmation
                                Toast.makeText(CartActivity.this, "Order placed successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(CartActivity.this, OrderConfirmationActivity.class);
                                intent.putExtra("orderId", orderId);
                                intent.putExtra("totalAmount", totalAmount);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding order items", e);
                                Toast.makeText(CartActivity.this, "Error processing order: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating order", e);
                    Toast.makeText(CartActivity.this, "Error creating order: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void clearCart() {
        for (CartItem item : new ArrayList<>(cartItems)) {
            firestore.collection("cart").document(item.getCartItemId())
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "Error removing cart item after order", e));
        }
    }
} 