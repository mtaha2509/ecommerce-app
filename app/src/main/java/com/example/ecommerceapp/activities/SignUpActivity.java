package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.ecommerceapp.MainActivity;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private EditText etEmail, etPassword;
    private Button btnSignUp;
    private TextView tvLogin;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Log.d(TAG, "onCreate started");

        // Get the role from WelcomeActivity
        userRole = getIntent().getStringExtra("role");
        if (userRole == null) {
            Log.e(TAG, "No role provided");
            Toast.makeText(this, "Error: No role selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();

            etEmail = findViewById(R.id.et_email);
            etPassword = findViewById(R.id.et_password);
            btnSignUp = findViewById(R.id.btn_sign_up);
            tvLogin = findViewById(R.id.tv_login);

            btnSignUp.setOnClickListener(v -> signUpUser());
            tvLogin.setOnClickListener(v -> {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void signUpUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Attempting to create user with email: " + email);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Store user role in Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("role", userRole);

                            Log.d(TAG, "Storing user data in Firestore for role: " + userRole);
                            firestore.collection("users")
                                    .document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User role stored in Firestore successfully");
                                        navigateBasedOnRole();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error storing user role in Firestore", e);
                                        Toast.makeText(SignUpActivity.this, 
                                            "Error storing user data: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.e(TAG, "Current user is null after successful signup");
                            Toast.makeText(SignUpActivity.this, 
                                "Error: User creation failed", 
                                Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "createUserWithEmail:failure", exception);
                        String errorMessage = "Authentication failed";
                        if (exception != null) {
                            errorMessage += ": " + exception.getMessage();
                        }
                        Toast.makeText(SignUpActivity.this, errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateBasedOnRole() {
        Log.d(TAG, "Navigating based on role: " + userRole);
        Intent intent;
        if (userRole.equals("seller")) {
            intent = new Intent(SignUpActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SignUpActivity.this, BuyerMainActivity.class);
        }
        startActivity(intent);
        finish();
    }
} 