package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.MyProductAdapter;
import com.example.ecommerceapp.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyProductsActivity extends AppCompatActivity implements MyProductAdapter.OnProductActionListener {
    private static final String TAG = "MyProductsActivity";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefresh;
    private MaterialToolbar toolbar;
    private FloatingActionButton fabAddProduct;
    
    private MyProductAdapter productAdapter;
    private List<Product> productList;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_products);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            finish();
            return;
        }
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        
        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Setup RecyclerView
        productList = new ArrayList<>();
        productAdapter = new MyProductAdapter(this, productList);
        productAdapter.setOnProductActionListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(productAdapter);
        
        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadProducts);
        
        // Setup FAB
        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProductActivity.class);
            startActivity(intent);
        });
        
        // Load products
        loadProducts();
    }
    
    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        // Query Firestore for products by this seller
        firestore.collection("products")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                    }
                    
                    productAdapter.notifyDataSetChanged();
                    
                    // Handle empty state
                    if (productList.isEmpty()) {
                        emptyView.setText("You haven't added any products yet");
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading products", e);
                    
                    emptyView.setText("Error loading products");
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                });
    }
    
    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("productId", product.getId());
        startActivity(intent);
    }
    
    @Override
    public void onEditClick(Product product) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("productId", product.getId());
        startActivity(intent);
    }
    
    @Override
    public void onDeleteClick(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProduct(product))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteProduct(Product product) {
        progressBar.setVisibility(View.VISIBLE);
        
        firestore.collection("products").document(product.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Snackbar.make(recyclerView, "Product deleted successfully", Snackbar.LENGTH_LONG).show();
                    loadProducts(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting product", e);
                    Snackbar.make(recyclerView, "Error deleting product", Snackbar.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh products when activity is resumed
        loadProducts();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 