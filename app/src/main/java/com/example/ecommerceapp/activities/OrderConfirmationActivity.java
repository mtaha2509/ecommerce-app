package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.MainActivity;
import com.example.ecommerceapp.activities.BuyerMainActivity;
import com.example.ecommerceapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderConfirmationActivity extends AppCompatActivity {
    
    private TextView tvOrderId, tvTotalAmount, tvConfirmationMessage;
    private Button btnContinueShopping;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);
        
        // Get order details from intent
        String orderId = getIntent().getStringExtra("orderId");
        double totalAmount = getIntent().getDoubleExtra("totalAmount", 0.0);
        
        // Initialize views
        tvOrderId = findViewById(R.id.tvOrderId);
        tvTotalAmount = findViewById(R.id.tvOrderTotal);
        tvConfirmationMessage = findViewById(R.id.tvConfirmationMessage);
        btnContinueShopping = findViewById(R.id.btnContinueShopping);
        
        // Set data
        tvOrderId.setText("Order #" + orderId);
        tvTotalAmount.setText(String.format("$%.2f", totalAmount));
        tvConfirmationMessage.setText("Thank you for your order! We have received your payment and will process your order shortly.");
        
        // Button click listener
        btnContinueShopping.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Check user role
                FirebaseFirestore.getInstance().collection("users")
                        .document(currentUser.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String role = documentSnapshot.getString("role");
                            Intent intent;
                            
                            if (role != null && role.equals("buyer")) {
                                // Direct buyers to the new BuyerMainActivity
                                intent = new Intent(OrderConfirmationActivity.this, BuyerMainActivity.class);
                            } else {
                                intent = new Intent(OrderConfirmationActivity.this, MainActivity.class);
                            }
                            
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            // Default to BuyerMainActivity on error
                            Intent intent = new Intent(OrderConfirmationActivity.this, BuyerMainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        });
            } else {
                // User not logged in, go to login
                Intent intent = new Intent(OrderConfirmationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
} 