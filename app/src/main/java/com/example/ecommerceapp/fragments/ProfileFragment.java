package com.example.ecommerceapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.activities.EditProfileActivity;
import com.example.ecommerceapp.activities.LoginActivity;
import com.example.ecommerceapp.activities.AddressManagementActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private TextView tvUserName, tvEmail, tvRole;
    private MaterialButton btnEditProfile, btnManageAddresses, btnLogout;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize views
        tvUserName = view.findViewById(R.id.tvUserName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvRole = view.findViewById(R.id.tvRole);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnManageAddresses = view.findViewById(R.id.btnManageAddresses);
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Set click listeners
        btnLogout.setOnClickListener(v -> logout());
        btnEditProfile.setOnClickListener(v -> editProfile());
        btnManageAddresses.setOnClickListener(v -> manageAddresses());
        
        // Load user profile
        loadUserProfile();
        
        return view;
    }
    
    private void loadUserProfile() {
        if (currentUser == null) {
            // User not logged in, redirect to login
            logout();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Set email from Firebase Auth
        tvEmail.setText(currentUser.getEmail());
        
        // Get additional user info from Firestore
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (documentSnapshot.exists()) {
                        // Set user name
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvUserName.setText(name);
                        } else {
                            tvUserName.setText("Anonymous User");
                        }
                        
                        // Set user role
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            tvRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
                        } else {
                            tvRole.setText("Buyer");
                        }
                    } else {
                        tvUserName.setText("Anonymous User");
                        tvRole.setText("Buyer");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading user profile", e);
                    Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }
    
    private void editProfile() {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivity(intent);
    }
    
    private void manageAddresses() {
        Intent intent = new Intent(getActivity(), AddressManagementActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }
}
