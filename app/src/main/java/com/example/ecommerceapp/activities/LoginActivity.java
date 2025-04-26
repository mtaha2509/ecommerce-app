package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.ecommerceapp.MainActivity;
import com.example.ecommerceapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate started");

        try {
            mAuth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase instances initialized");

            // Check if user is already logged in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "User already logged in, checking role");
                checkUserRole(currentUser.getUid());
                return;
            }

            initializeViews();
            setupClickListeners();
            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkUserRole(String userId) {
        Log.d(TAG, "Checking role for user: " + userId);
        try {
            firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Log.d(TAG, "Firestore query successful");
                        try {
                            if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                                String role = documentSnapshot.getString("role");
                                Log.d(TAG, "User role found: " + role);
                                if (role != null) {
                                    Intent intent;
                                    if (role.equals("buyer")) {
                                        intent = new Intent(LoginActivity.this, ProductListActivity.class);
                                    } else {
                                        intent = new Intent(LoginActivity.this, MainActivity.class);
                                    }
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Log.e(TAG, "No role set for user");
                                    Toast.makeText(LoginActivity.this, "Error: No role set for user", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                }
                            } else {
                                Log.e(TAG, "No role document found");
                                Toast.makeText(LoginActivity.this, "Error: No role found", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing role", e);
                            Toast.makeText(LoginActivity.this, "Error processing role", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking role", e);
                        Toast.makeText(LoginActivity.this, "Error checking role", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in checkUserRole", e);
            Toast.makeText(this, "Error checking role", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");
        try {
            emailEditText = findViewById(R.id.et_email);
            passwordEditText = findViewById(R.id.et_password);
            loginButton = findViewById(R.id.btn_login);
            signUpTextView = findViewById(R.id.tv_sign_up);
            progressBar = findViewById(R.id.progress_bar);
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        try {
            loginButton.setOnClickListener(v -> loginUser());
            signUpTextView.setOnClickListener(v -> startSignUpActivity());
            Log.d(TAG, "Click listeners setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
            Toast.makeText(this, "Error setting up buttons", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginUser() {
        Log.d(TAG, "Login attempt started");
        try {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Email is required");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Password is required");
                return;
            }

            if (password.length() < 6) {
                passwordEditText.setError("Password must be at least 6 characters");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Starting Firebase authentication");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                checkUserRole(user.getUid());
                            } else {
                                Log.e(TAG, "User is null after successful login");
                                Toast.makeText(LoginActivity.this, "Error: User is null", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Login failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in loginUser", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error during login: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startSignUpActivity() {
        Log.d(TAG, "Starting sign up activity");
        try {
            startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting sign up activity", e);
            Toast.makeText(this, "Error starting sign up", Toast.LENGTH_SHORT).show();
        }
    }
} 