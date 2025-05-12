package com.example.ecommerceapp.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.ImagePagerAdapter;
import com.example.ecommerceapp.models.CartItem;
import com.example.ecommerceapp.models.Conversation;
import com.example.ecommerceapp.models.Review;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailActivity";
    private ViewPager2 viewPager;
    private WormDotsIndicator dotsIndicator;
    private MaterialCardView cardVideo360;
    private FloatingActionButton fab3DView;
    private TextView tvView360;
    private TextView tvProductTitle, tvProductDescription, tvProductPrice, tvQuantity;
    private TextView tvRatingCount, tvViewReviews, tvViewAllReviews, tvAllReviewsEmpty, tvWriteReview, tvNoReviews;
    private RatingBar ratingBar;
    private Button btnAddToCart, btnViewCart, btnDecrease, btnIncrease, btnContactSeller;
    private MaterialToolbar toolbar;
    private View topReviewPreview;
    private ConstraintLayout noReviewsContainer;
    
    private Dialog videoDialog;
    private ExoPlayer exoPlayer;
    private Animation pulseAnimation;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String productId;
    private String productTitle;
    private double productPrice;
    private List<String> imageUrls;
    private String videoUrl;
    private int quantity = 1;
    private String sellerId;
    private float averageRating = 0;
    private int reviewCount = 0;
    private Review topReview;

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
            setupToolbar();
            initializeVideoDialog();
            loadProductDetails();
            
            // Load pulse animation
            pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
            fab3DView.startAnimation(pulseAnimation);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    
    private void initializeVideoDialog() {
        videoDialog = new Dialog(this);
        videoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        videoDialog.setContentView(R.layout.dialog_video_player);
        videoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Initialize ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build();
        
        StyledPlayerView videoPlayer = videoDialog.findViewById(R.id.videoPlayer);
        videoPlayer.setPlayer(exoPlayer);
        videoPlayer.setControllerShowTimeoutMs(3000);
        videoPlayer.setControllerHideOnTouch(true);
        
        // Set listener to handle errors
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                Log.e(TAG, "ExoPlayer error: " + error.getMessage());
                Toast.makeText(ProductDetailActivity.this, 
                        "Error playing video. Please try again later.", Toast.LENGTH_SHORT).show();
                exoPlayer.stop();
                videoDialog.dismiss();
            }
        });
        
        // Set close button click listener
        ImageButton btnCloseVideo = videoDialog.findViewById(R.id.btnCloseVideo);
        btnCloseVideo.setOnClickListener(v -> {
            exoPlayer.stop();
            videoDialog.dismiss();
        });
        
        // Set dialog dismiss listener to stop the video
        videoDialog.setOnDismissListener(dialog -> exoPlayer.stop());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        viewPager = findViewById(R.id.viewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        cardVideo360 = findViewById(R.id.cardVideo360);
        fab3DView = findViewById(R.id.fabView3D);
        tvView360 = findViewById(R.id.tvView360);
        tvProductTitle = findViewById(R.id.tvProductTitle);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvProductDescription = findViewById(R.id.tvProductDescription);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnViewCart = findViewById(R.id.btnViewCart);
        btnDecrease = findViewById(R.id.btnDecrease);
        btnIncrease = findViewById(R.id.btnIncrease);
        tvQuantity = findViewById(R.id.tvQuantity);
        btnContactSeller = findViewById(R.id.btnContactSeller);
        ratingBar = findViewById(R.id.ratingBar);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvViewReviews = findViewById(R.id.tvViewReviews);
        tvViewAllReviews = findViewById(R.id.tvViewAllReviews);
        tvAllReviewsEmpty = findViewById(R.id.tvAllReviewsEmpty);
        tvWriteReview = findViewById(R.id.tvWriteReview);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        topReviewPreview = findViewById(R.id.topReviewPreview);
        noReviewsContainer = findViewById(R.id.noReviewsContainer);

        // Set initial quantity
        tvQuantity.setText(String.valueOf(quantity));

        // Set click listeners
        btnAddToCart.setOnClickListener(v -> addToCart());
        btnViewCart.setOnClickListener(v -> viewCart());
        btnIncrease.setOnClickListener(v -> increaseQuantity());
        btnDecrease.setOnClickListener(v -> decreaseQuantity());
        btnContactSeller.setOnClickListener(v -> initiateChat());
        fab3DView.setOnClickListener(v -> showVideoPlayer());
        tvView360.setOnClickListener(v -> showVideoPlayer());
        tvViewReviews.setOnClickListener(v -> openReviewsActivity());
        tvViewAllReviews.setOnClickListener(v -> openReviewsActivity());
        tvAllReviewsEmpty.setOnClickListener(v -> openReviewsActivity());
        tvWriteReview.setOnClickListener(v -> openAddReviewActivity());
    }

    private void showVideoPlayer() {
        if (videoUrl != null && !videoUrl.isEmpty()) {
            // First validate URL format
            if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
                Toast.makeText(this, "Invalid video URL format", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                // Reset player to avoid carrying over previous errors
                exoPlayer.stop();
                exoPlayer.clearMediaItems();
                
                // Create new media item and prepare player
                MediaItem mediaItem = MediaItem.fromUri(videoUrl);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
                
                videoDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error playing video", e);
                Toast.makeText(this, "Error playing video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No 360° view available for this product", Toast.LENGTH_SHORT).show();
            fab3DView.clearAnimation();
        }
    }

    private void openReviewsActivity() {
        Intent intent = new Intent(this, ReviewsActivity.class);
        intent.putExtra("productId", productId);
        intent.putExtra("productTitle", productTitle);
        startActivity(intent);
    }

    private void openAddReviewActivity() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && sellerId != null && !currentUser.getUid().equals(sellerId)) {
            Intent intent = new Intent(this, AddReviewActivity.class);
            intent.putExtra("productId", productId);
            intent.putExtra("productTitle", productTitle);
            startActivity(intent);
        } else if (sellerId != null && currentUser != null && currentUser.getUid().equals(sellerId)) {
            Toast.makeText(this, "You cannot review your own product", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please login to add a review", Toast.LENGTH_SHORT).show();
        }
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
                        videoUrl = documentSnapshot.getString("videoUrl");
                        sellerId = documentSnapshot.getString("userId");
                        
                        // Get rating info if available
                        if (documentSnapshot.contains("averageRating")) {
                            averageRating = documentSnapshot.getDouble("averageRating").floatValue();
                        }
                        
                        if (documentSnapshot.contains("reviewCount")) {
                            reviewCount = documentSnapshot.getLong("reviewCount").intValue();
                        }

                        // Set product details
                        tvProductTitle.setText(productTitle);
                        tvProductDescription.setText(description);
                        tvProductPrice.setText(String.format("$%.2f", productPrice));

                        // Setup image slider
                        if (imageUrls != null && !imageUrls.isEmpty()) {
                            setupImageSlider(imageUrls);
                        }

                        // Show/hide 360 view button
                        if (videoUrl == null || videoUrl.isEmpty()) {
                            fab3DView.setVisibility(View.GONE);
                            cardVideo360.setVisibility(View.GONE);
                        } else {
                            fab3DView.setVisibility(View.VISIBLE);
                            cardVideo360.setVisibility(View.GONE);
                        }

                        // Update rating display
                        updateRatingDisplay();

                        // Load top review
                        loadTopReview();

                        // Setup contact seller button
                        setupContactButton();
                    } else {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading product details", e);
                    Toast.makeText(this, "Error loading product details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void setupImageSlider(List<String> images) {
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, images);
        viewPager.setAdapter(adapter);
        
        // Configure tab layout indicators with proper spacing
        dotsIndicator.setViewPager2(viewPager);
        
        // Reduce offscreen page limit to improve performance
        viewPager.setOffscreenPageLimit(1);
        
        // Optional: add page change listener for additional effects
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // You could do something when page changes
                // For example, log analytics or update other UI elements
            }
        });
    }
    
    private void updateRatingDisplay() {
        if (reviewCount > 0) {
            ratingBar.setRating(averageRating);
            tvRatingCount.setText(String.format("%.1f (%d reviews)", averageRating, reviewCount));
            noReviewsContainer.setVisibility(View.GONE);
            tvViewAllReviews.setVisibility(View.VISIBLE);
        } else {
            ratingBar.setRating(0);
            tvRatingCount.setText("(No reviews yet)");
            noReviewsContainer.setVisibility(View.VISIBLE);
            tvViewAllReviews.setVisibility(View.GONE);
        }
    }
    
    private void loadTopReview() {
        if (reviewCount > 0) {
            firestore.collection("reviews")
                    .whereEqualTo("productId", productId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            topReview = doc.toObject(Review.class);
                            
                            // Set the top review preview
                            TextView tvUsername = topReviewPreview.findViewById(R.id.tv_username);
                            TextView tvComment = topReviewPreview.findViewById(R.id.tv_comment);
                            RatingBar reviewRatingBar = topReviewPreview.findViewById(R.id.rating_bar);
                            
                            tvUsername.setText(topReview.getUserName());
                            tvComment.setText(topReview.getComment());
                            reviewRatingBar.setRating(topReview.getRating());
                            
                            topReviewPreview.setVisibility(View.VISIBLE);
                        } else {
                            topReviewPreview.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading top review", e);
                        topReviewPreview.setVisibility(View.GONE);
                    });
        } else {
            topReviewPreview.setVisibility(View.GONE);
        }
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
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh reviews when coming back from adding a review
        if (productId != null) {
            loadProductDetails();
        }
    }
} 