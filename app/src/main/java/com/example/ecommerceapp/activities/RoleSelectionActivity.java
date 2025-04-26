package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.MainActivity;
import com.example.ecommerceapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoleSelectionActivity extends AppCompatActivity {
    private Button btnSeller, btnBuyer;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnSeller = findViewById(R.id.btn_seller);
        btnBuyer = findViewById(R.id.btn_buyer);

        btnSeller.setOnClickListener(v -> {
            // Save user role as seller
            saveUserRole("seller");
            startActivity(new Intent(RoleSelectionActivity.this, MainActivity.class));
            finish();
        });

        btnBuyer.setOnClickListener(v -> {
            // Save user role as buyer
            saveUserRole("buyer");
            startActivity(new Intent(RoleSelectionActivity.this, ProductListActivity.class));
            finish();
        });
    }

    private void saveUserRole(String role) {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("users")
                .document(userId)
                .update("role", role)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Role set as " + role, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error setting role", Toast.LENGTH_SHORT).show();
                });
    }
} 