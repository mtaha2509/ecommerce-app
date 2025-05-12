package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.ReviewAdapter;
import com.example.ecommerceapp.models.Review;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewsActivity extends AppCompatActivity {
    private static final String TAG = "ReviewsActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefresh;
    private MaterialToolbar toolbar;
    
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private FirebaseFirestore firestore;
    private String productId;
    private String productTitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);
        
        // Get intent data
        productId = getIntent().getStringExtra("productId");
        productTitle = getIntent().getStringExtra("productTitle");
        
        if (productId == null) {
            finish();
            return;
        }
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerViewReviews);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reviews for " + productTitle);
        }
        
        // Setup RecyclerView
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(reviewAdapter);
        
        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadReviews);
        
        // Load reviews
        loadReviews();
    }
    
    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        firestore.collection("reviews")
                .whereEqualTo("productId", productId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Review review = document.toObject(Review.class);
                        review.setId(document.getId());
                        reviewList.add(review);
                    }
                    
                    reviewAdapter.notifyDataSetChanged();
                    
                    // Handle empty state
                    if (reviewList.isEmpty()) {
                        emptyView.setText("No reviews yet for this product");
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                    emptyView.setText("Error loading reviews");
                    emptyView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 