package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.ImagePagerAdapter;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailActivity";
    private ViewPager2 viewPager;
    private StyledPlayerView videoPlayer;
    private TextView tvProductTitle, tvProductPrice, tvProductDescription;
    private Button btnAddToCart;
    private ExoPlayer exoPlayer;
    private FirebaseFirestore firestore;
    private String productId;

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

        // Initialize ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        videoPlayer.setPlayer(exoPlayer);

        btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void loadProductDetails() {
        firestore.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String description = documentSnapshot.getString("description");
                        double price = documentSnapshot.getDouble("price");
                        List<String> imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                        String videoUrl = documentSnapshot.getString("videoUrl");

                        tvProductTitle.setText(title);
                        tvProductDescription.setText(description);
                        tvProductPrice.setText(String.format("$%.2f", price));

                        // Set up image carousel
                        if (imageUrls != null && !imageUrls.isEmpty()) {
                            ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageUrls);
                            viewPager.setAdapter(adapter);
                        }

                        // Set up video player if video URL exists
                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            videoPlayer.setVisibility(android.view.View.VISIBLE);
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
        // TODO: Implement add to cart functionality
        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
    }
} 