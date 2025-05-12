package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    
    private EditText etName, etPhone, etBio;
    private EditText etBusinessName, etBusinessDescription;
    private LinearLayout sellerInfoLayout;
    private Button btnSave;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private User userProfile;
    private String userRole;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Profile");
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        if (currentUser == null) {
            finish();
            return;
        }
        
        // Initialize views
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etBio = findViewById(R.id.etBio);
        etBusinessName = findViewById(R.id.etBusinessName);
        etBusinessDescription = findViewById(R.id.etBusinessDescription);
        sellerInfoLayout = findViewById(R.id.sellerInfoLayout);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up click listeners
        btnSave.setOnClickListener(v -> saveProfile());
        
        // Load user profile
        loadUserProfile();
    }
    
    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (documentSnapshot.exists()) {
                        userProfile = documentSnapshot.toObject(User.class);
                        userProfile.setId(documentSnapshot.getId());
                        
                        // Set user role
                        userRole = userProfile.getRole();
                        
                        // Populate fields
                        populateFields();
                        
                        // Show seller fields if user is a seller
                        if (userProfile.isSeller()) {
                            sellerInfoLayout.setVisibility(View.VISIBLE);
                        } else {
                            sellerInfoLayout.setVisibility(View.GONE);
                        }
                    } else {
                        userProfile = new User();
                        userProfile.setId(currentUser.getUid());
                        userProfile.setEmail(currentUser.getEmail());
                        userRole = "buyer"; // Default role
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading user profile", e);
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void populateFields() {
        if (userProfile != null) {
            etName.setText(userProfile.getName());
            etPhone.setText(userProfile.getPhone());
            etBio.setText(userProfile.getBio());
            
            if (userProfile.isSeller()) {
                etBusinessName.setText(userProfile.getBusinessName());
                etBusinessDescription.setText(userProfile.getBusinessDescription());
            }
        }
    }
    
    private void saveProfile() {
        if (currentUser == null) return;
        
        // Validate form
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Create map of fields to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", etPhone.getText().toString().trim());
        updates.put("bio", etBio.getText().toString().trim());
        
        // Add seller-specific fields if user is a seller
        if ("seller".equals(userRole)) {
            String businessName = etBusinessName.getText().toString().trim();
            if (businessName.isEmpty()) {
                etBusinessName.setError("Business name is required");
                etBusinessName.requestFocus();
                progressBar.setVisibility(View.GONE);
                return;
            }
            
            updates.put("businessName", businessName);
            updates.put("businessDescription", etBusinessDescription.getText().toString().trim());
        }
        
        // Update profile in Firestore
        firestore.collection("users")
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 