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

                            firestore.collection("users")
                                    .document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User role stored in Firestore");
                                        navigateBasedOnRole();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error storing user role", e);
                                        Toast.makeText(SignUpActivity.this, "Error storing user data", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateBasedOnRole() {
        Intent intent;
        if (userRole.equals("buyer")) {
            intent = new Intent(SignUpActivity.this, ProductListActivity.class);
        } else {
            intent = new Intent(SignUpActivity.this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
} 