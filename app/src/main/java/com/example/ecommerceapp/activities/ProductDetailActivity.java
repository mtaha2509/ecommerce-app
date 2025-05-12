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
import com.example.ecommerceapp.models.Conversation;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailActivity";
    private ViewPager2 viewPager;
    private StyledPlayerView videoPlayer;
    private TextView tvProductTitle, tvProductPrice, tvProductDescription, tvQuantity;
    private Button btnAddToCart, btnViewCart, btnDecrease, btnIncrease, btnContactSeller;
    private ExoPlayer exoPlayer;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String productId;
    private String productTitle;
    private double productPrice;
    private List<String> imageUrls;
    private int quantity = 1;
    private String sellerId;

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
        btnContactSeller = findViewById(R.id.btnContactSeller);

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
        btnContactSeller.setOnClickListener(v -> initiateChat());
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
                        sellerId = documentSnapshot.getString("userId");

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
                        
                        // Setup contact seller button visibility
                        setupContactButton();
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
    
    private void setupContactButton() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        // Only show contact button for buyers (hide for sellers)
        if (currentUser != null && !currentUser.getUid().equals(sellerId)) {
            btnContactSeller.setVisibility(View.VISIBLE);
        } else {
            btnContactSeller.setVisibility(View.GONE);
        }
    }
    
    private void initiateChat() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to contact the seller", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // First check if a conversation already exists
        firestore.collection("conversations")
                .whereEqualTo("buyerId", currentUser.getUid())
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Conversation exists, open it
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Conversation conversation = doc.toObject(Conversation.class);
                        conversation.setId(doc.getId());
                        openChatActivity(conversation);
                    } else {
                        // Create new conversation
                        createNewConversation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing conversations", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void createNewConversation() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        // First get seller's name
        firestore.collection("users").document(sellerId)
                .get()
                .addOnSuccessListener(sellerDoc -> {
                    if (sellerDoc.exists()) {
                        // Get buyer's name
                        firestore.collection("users").document(currentUser.getUid())
                                .get()
                                .addOnSuccessListener(buyerDoc -> {
                                    if (buyerDoc.exists()) {
                                        String sellerName = sellerDoc.getString("name");
                                        String buyerName = buyerDoc.getString("name");
                                        
                                        if (sellerName == null) sellerName = "Seller";
                                        if (buyerName == null) buyerName = "Buyer";
                                        
                                        // Create conversation
                                        Conversation conversation = new Conversation(
                                                currentUser.getUid(),
                                                sellerId,
                                                buyerName,
                                                sellerName,
                                                productId,
                                                productTitle
                                        );
                                        
                                        // Add to Firestore
                                        firestore.collection("conversations")
                                                .add(conversation)
                                                .addOnSuccessListener(documentReference -> {
                                                    conversation.setId(documentReference.getId());
                                                    openChatActivity(conversation);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Error creating conversation", e);
                                                    Toast.makeText(ProductDetailActivity.this, 
                                                            "Failed to create conversation", 
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Could not find seller information", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting seller information", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void openChatActivity(Conversation conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversation.getId());
        intent.putExtra("otherUserName", conversation.getSellerName());
        intent.putExtra("productId", conversation.getProductId());
        intent.putExtra("productTitle", conversation.getProductTitle());
        startActivity(intent);
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
                sellerId,
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
        
        // Update both quantity and sellerId to ensure it's set
        Map<String, Object> updates = new HashMap<>();
        updates.put("quantity", newQuantity);
        updates.put("sellerId", sellerId);
        
        firestore.collection("cart").document(cartItemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cart item quantity and seller ID updated");
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