package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.MainActivity;
import com.example.ecommerceapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 1000; // 1 second
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        try {
            mAuth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
            
            // Add a slight delay to show splash screen
            new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthState, SPLASH_DURATION);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
            goToWelcomeScreen();
        }
    }
    
    private void checkAuthState() {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "User already logged in, checking role");
                checkUserRole(currentUser.getUid());
            } else {
                Log.d(TAG, "No user logged in");
                goToWelcomeScreen();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking auth state", e);
            goToWelcomeScreen();
        }
    }
    
    private void checkUserRole(String userId) {
        Log.d(TAG, "Checking role for user: " + userId);
        try {
            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                                String role = documentSnapshot.getString("role");
                                Log.d(TAG, "User role found: " + role);
                                if (role != null) {
                                    if (role.equals("buyer")) {
                                        startActivity(new Intent(SplashActivity.this, BuyerMainActivity.class));
                                    } else {
                                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                    }
                                    finish();
                                } else {
                                    Log.e(TAG, "No role set for user");
                                    mAuth.signOut();
                                    goToWelcomeScreen();
                                }
                            } else {
                                Log.e(TAG, "No role document found");
                                mAuth.signOut();
                                goToWelcomeScreen();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing role", e);
                            mAuth.signOut();
                            goToWelcomeScreen();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking role", e);
                        mAuth.signOut();
                        goToWelcomeScreen();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in checkUserRole", e);
            mAuth.signOut();
            goToWelcomeScreen();
        }
    }
    
    private void goToWelcomeScreen() {
        startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
        finish();
    }
} 