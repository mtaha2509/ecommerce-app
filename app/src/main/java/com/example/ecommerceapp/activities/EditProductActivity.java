package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProductActivity extends AppCompatActivity {
    private static final String TAG = "EditProductActivity";

    private MaterialToolbar toolbar;
    private EditText etTitle, etDescription, etPrice;
    private Spinner spCategory;
    private Button btnSave;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private String productId;
    private String title, description, category;
    private double price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        // Get product ID from intent
        productId = getIntent().getStringExtra("productId");
        if (productId == null) {
            Toast.makeText(this, "Error: Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Product");
        }

        // Setup category spinner
        setupCategorySpinner();

        // Load product details
        loadProductDetails();

        // Setup save button
        btnSave.setOnClickListener(v -> saveProductChanges());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        etPrice = findViewById(R.id.et_price);
        spCategory = findViewById(R.id.sp_category);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.product_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
    }

    private void loadProductDetails() {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get product details
                        title = documentSnapshot.getString("title");
                        description = documentSnapshot.getString("description");
                        category = documentSnapshot.getString("category");
                        if (documentSnapshot.contains("price")) {
                            price = documentSnapshot.getDouble("price");
                        }

                        // Populate fields
                        etTitle.setText(title);
                        etDescription.setText(description);
                        etPrice.setText(String.valueOf(price));

                        // Set category in spinner
                        ArrayAdapter adapter = (ArrayAdapter) spCategory.getAdapter();
                        if (category != null) {
                            int position = adapter.getPosition(category);
                            if (position >= 0) {
                                spCategory.setSelection(position);
                            }
                        }

                        progressBar.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading product details", e);
                    Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    finish();
                });
    }

    private void saveProductChanges() {
        // Validate input
        String newTitle = etTitle.getText().toString().trim();
        String newDescription = etDescription.getText().toString().trim();
        String newCategory = spCategory.getSelectedItem().toString();
        String priceStr = etPrice.getText().toString().trim();

        if (newTitle.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (newDescription.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (priceStr.isEmpty()) {
            etPrice.setError("Price is required");
            etPrice.requestFocus();
            return;
        }

        // Parse price
        double newPrice;
        try {
            newPrice = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etPrice.setError("Invalid price format");
            etPrice.requestFocus();
            return;
        }

        // Update UI state
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        // Create update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", newTitle);
        updates.put("description", newDescription);
        updates.put("category", newCategory);
        updates.put("price", newPrice);

        // Update in Firestore
        firestore.collection("products").document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating product", e);
                    Toast.makeText(this, "Error updating product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 