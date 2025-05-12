package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Review;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddReviewActivity extends AppCompatActivity {
    private static final String TAG = "AddReviewActivity";
    
    private EditText etComment;
    private RatingBar ratingBar;
    private Button btnSubmit;
    private MaterialToolbar toolbar;
    
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String productId;
    private String productTitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review);
        
        // Get intent data
        productId = getIntent().getStringExtra("productId");
        productTitle = getIntent().getStringExtra("productTitle");
        
        if (productId == null) {
            finish();
            return;
        }
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        
        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to add a review", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        etComment = findViewById(R.id.et_comment);
        ratingBar = findViewById(R.id.rating_bar);
        btnSubmit = findViewById(R.id.btn_submit);
        
        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Review " + productTitle);
        }
        
        // Setup submit button
        btnSubmit.setOnClickListener(v -> submitReview());
    }
    
    private void submitReview() {
        FirebaseUser currentUser = auth.getCurrentUser();
        String comment = etComment.getText().toString().trim();
        float rating = ratingBar.getRating();
        
        if (comment.isEmpty()) {
            etComment.setError("Please enter a comment");
            return;
        }
        
        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button and show loading state
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");
        
        // First get user's name
        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = documentSnapshot.getString("name");
                    if (userName == null || userName.isEmpty()) {
                        userName = "Anonymous";
                    }
                    
                    // Create review object
                    Review review = new Review(
                            productId,
                            currentUser.getUid(),
                            userName,
                            comment,
                            rating
                    );
                    
                    // Add to Firestore
                    firestore.collection("reviews")
                            .add(review)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Review added successfully");
                                Toast.makeText(AddReviewActivity.this, "Review submitted successfully", Toast.LENGTH_SHORT).show();
                                
                                // Update product average rating
                                updateProductRating();
                                
                                // Close activity
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding review", e);
                                Toast.makeText(AddReviewActivity.this, "Error submitting review", Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Submit Review");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user data", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Review");
                });
    }
    
    private void updateProductRating() {
        // Calculate average rating for this product
        firestore.collection("reviews")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        float totalRating = 0;
                        int count = 0;
                        
                        for (Review review : queryDocumentSnapshots.toObjects(Review.class)) {
                            totalRating += review.getRating();
                            count++;
                        }
                        
                        float averageRating = totalRating / count;
                        
                        // Update product with new average rating
                        firestore.collection("products").document(productId)
                                .update("averageRating", averageRating, 
                                       "reviewCount", count)
                                .addOnSuccessListener(aVoid -> 
                                    Log.d(TAG, "Product rating updated successfully"))
                                .addOnFailureListener(e -> 
                                    Log.e(TAG, "Error updating product rating", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error calculating average rating", e));
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 