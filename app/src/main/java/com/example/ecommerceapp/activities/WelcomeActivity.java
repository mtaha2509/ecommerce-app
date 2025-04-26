package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.R;

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";
    private Button btnSeller, btnBuyer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Log.d(TAG, "onCreate started");

        try {
            btnSeller = findViewById(R.id.btn_seller);
            btnBuyer = findViewById(R.id.btn_buyer);

            if (btnSeller == null || btnBuyer == null) {
                Log.e(TAG, "Buttons not found in layout");
                Toast.makeText(this, "Error initializing buttons", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSeller.setOnClickListener(v -> {
                Log.d(TAG, "Seller button clicked");
                try {
                    Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
                    intent.putExtra("role", "seller");
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting SignUpActivity for seller", e);
                    Toast.makeText(this, "Error starting sign up", Toast.LENGTH_SHORT).show();
                }
            });

            btnBuyer.setOnClickListener(v -> {
                Log.d(TAG, "Buyer button clicked");
                try {
                    Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
                    intent.putExtra("role", "buyer");
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting SignUpActivity for buyer", e);
                    Toast.makeText(this, "Error starting sign up", Toast.LENGTH_SHORT).show();
                }
            });

            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 