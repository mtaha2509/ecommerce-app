package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.ImagePagerAdapter;
import com.example.ecommerceapp.models.CartItem;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailActivity";
    private ViewPager2 viewPager;
    private StyledPlayerView videoPlayer;
    private TextView tvProductTitle, tvProductPrice, tvProductDescription, tvQuantity;
    private Button btnAddToCart, btnViewCart, btnDecrease, btnIncrease;
    private ExoPlayer exoPlayer;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String productId;
    private String productTitle;
    private double productPrice;
    private List<String> imageUrls;
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        Log.d(TAG, "onCreate started");

        try {
            productId = getIntent().getStringExtra("productId");
            if (productId == null) {
                Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            firestore = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            
            // Check if user is logged in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                // Redirect to login
                Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            initializeViews();
            loadProductDetails();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        videoPlayer = findViewById(R.id.videoPlayer);
        tvProductTitle = findViewById(R.id.tvProductTitle);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvProductDescription = findViewById(R.id.tvProductDescription);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnViewCart = findViewById(R.id.btnViewCart);
        btnDecrease = findViewById(R.id.btnDecrease);
        btnIncrease = findViewById(R.id.btnIncrease);
        tvQuantity = findViewById(R.id.tvQuantity);

        // Initialize ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        videoPlayer.setPlayer(exoPlayer);

        // Set initial quantity
        tvQuantity.setText(String.valueOf(quantity));

        // Set click listeners
        btnAddToCart.setOnClickListener(v -> addToCart());
        btnViewCart.setOnClickListener(v -> viewCart());
        btnIncrease.setOnClickListener(v -> increaseQuantity());
        btnDecrease.setOnClickListener(v -> decreaseQuantity());
    }

    private void increaseQuantity() {
        quantity++;
        tvQuantity.setText(String.valueOf(quantity));
    }

    private void decreaseQuantity() {
        if (quantity > 1) {
            quantity--;
            tvQuantity.setText(String.valueOf(quantity));
        }
    }

    private void loadProductDetails() {
        firestore.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        productTitle = documentSnapshot.getString("title");
                        String description = documentSnapshot.getString("description");
                        productPrice = documentSnapshot.getDouble("price");
                        imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                        String videoUrl = documentSnapshot.getString("videoUrl");

                        tvProductTitle.setText(productTitle);
                        tvProductDescription.setText(description);
                        tvProductPrice.setText(String.format("$%.2f", productPrice));

                        // Set up image carousel
                        if (imageUrls != null && !imageUrls.isEmpty()) {
                            ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageUrls);
                            viewPager.setAdapter(adapter);
                        }

                        // Set up video player if video URL exists
                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            videoPlayer.setVisibility(View.VISIBLE);
                            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
                            exoPlayer.setMediaItem(mediaItem);
                            exoPlayer.prepare();
                        }
                    } else {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading product details", e);
                    Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show();
                });
    }

    private void addToCart() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to add items to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        
        // First, check if this product already exists in the user's cart
        firestore.collection("cart")
                .whereEqualTo("userId", userId)
                .whereEqualTo("productId", productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Product not in cart, add new item
                            addNewCartItem(userId);
                        } else {
                            // Product already in cart, update quantity
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                updateCartItemQuantity(document.getId(), document.getLong("quantity").intValue());
                                break; // should only be one matching document
                            }
                        }
                    } else {
                        Log.e(TAG, "Error checking cart", task.getException());
                        Toast.makeText(ProductDetailActivity.this, "Error: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void addNewCartItem(String userId) {
        String imageUrl = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : "";
        
        CartItem cartItem = new CartItem(
                productId,
                userId,
                productTitle,
                productPrice,
                imageUrl,
                quantity
        );
        
        firestore.collection("cart")
                .add(cartItem)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Item added to cart with ID: " + documentReference.getId());
                    Snackbar.make(btnAddToCart, "Added to cart", Snackbar.LENGTH_LONG)
                            .setAction("VIEW CART", v -> viewCart())
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding to cart", e);
                    Toast.makeText(ProductDetailActivity.this, "Error adding to cart: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateCartItemQuantity(String cartItemId, int currentQuantity) {
        int newQuantity = currentQuantity + quantity;
        
        firestore.collection("cart").document(cartItemId)
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cart item quantity updated");
                    Snackbar.make(btnAddToCart, "Cart updated", Snackbar.LENGTH_LONG)
                            .setAction("VIEW CART", v -> viewCart())
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating cart", e);
                    Toast.makeText(ProductDetailActivity.this, "Error updating cart: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void viewCart() {
        Intent intent = new Intent(this, CartActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
    }
} 