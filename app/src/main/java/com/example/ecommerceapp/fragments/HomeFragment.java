package com.example.ecommerceapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.activities.AddProductActivity;
import com.example.ecommerceapp.activities.OrderManagementActivity;
import com.example.ecommerceapp.adapters.SellerOrderAdapter;
import com.example.ecommerceapp.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements SellerOrderAdapter.SellerOrderClickListener {
    private static final String TAG = "HomeFragment";
    
    private TextView tvSellerWelcome, tvSummary;
    private TextView tvPendingOrderCount, tvProcessingOrderCount, tvShippedOrderCount;
    private Button btnAddProduct, btnManageOrders, btnViewAllOrders;
    private RecyclerView recyclerViewRecentOrders;
    private TextView tvNoSalesData;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    private List<Order> recentOrders;
    private SellerOrderAdapter orderAdapter;
    
    // Track if we're currently loading data to prevent duplicates
    private boolean isLoadingData = false;
    
    // Keep track of fragment lifecycle state
    private boolean isFragmentActive = false;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "onViewCreated called");
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to view dashboard", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Initialize views
        initializeViews(view);
        
        // Set seller welcome message
        setSellerWelcome();
        
        // Set up RecyclerView for recent orders
        setupRecyclerView();
        
        // Set click listeners
        setupClickListeners();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        isFragmentActive = true;
        
        if (currentUser != null) {
            // Load data only if we're not already loading
            if (!isLoadingData) {
                loadData();
            }
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        isFragmentActive = false;
    }
    
    private void loadData() {
        if (!isFragmentActive) return;
        
        isLoadingData = true;
        Log.d(TAG, "Loading data started");
        
        // Reset UI first
        resetOrderCounts();
        
        // Load recent orders and order counts
        loadRecentOrders();
        loadOrderCounts();
    }
    
    private void resetOrderCounts() {
        if (tvPendingOrderCount != null) tvPendingOrderCount.setText("0");
        if (tvProcessingOrderCount != null) tvProcessingOrderCount.setText("0");
        if (tvShippedOrderCount != null) tvShippedOrderCount.setText("0");
    }
    
    private void initializeViews(View view) {
        tvSellerWelcome = view.findViewById(R.id.tvSellerWelcome);
        tvSummary = view.findViewById(R.id.tvSummary);
        
        tvPendingOrderCount = view.findViewById(R.id.tvPendingOrderCount);
        tvProcessingOrderCount = view.findViewById(R.id.tvProcessingOrderCount);
        tvShippedOrderCount = view.findViewById(R.id.tvShippedOrderCount);
        
        btnAddProduct = view.findViewById(R.id.btnAddProduct);
        btnManageOrders = view.findViewById(R.id.btnManageOrders);
        btnViewAllOrders = view.findViewById(R.id.btnViewAllOrders);
        
        recyclerViewRecentOrders = view.findViewById(R.id.recyclerViewRecentOrders);
        tvNoSalesData = view.findViewById(R.id.tvNoSalesData);
    }
    
    private void setSellerWelcome() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String email = documentSnapshot.getString("email");
                        String username = email != null ? email.split("@")[0] : "Seller";
                        
                        // Capitalize first letter
                        if (username != null && !username.isEmpty()) {
                            username = username.substring(0, 1).toUpperCase() + username.substring(1);
                        }
                        
                        tvSellerWelcome.setText("Welcome back, " + username);
                    }
                });
    }
    
    private void setupRecyclerView() {
        recentOrders = new ArrayList<>();
        recyclerViewRecentOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new SellerOrderAdapter(getContext(), recentOrders, this);
        recyclerViewRecentOrders.setAdapter(orderAdapter);
    }
    
    private void setupClickListeners() {
        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddProductActivity.class);
            startActivity(intent);
        });
        
        btnManageOrders.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OrderManagementActivity.class);
            startActivity(intent);
        });
        
        btnViewAllOrders.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OrderManagementActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadRecentOrders() {
        if (!isFragmentActive) return;
        
        Log.d(TAG, "Loading recent orders");
        
        // Load recent orders for this seller
        firestore.collection("orders")
                .whereArrayContains("sellerIds", currentUser.getUid())
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .limit(5) // Only get 5 most recent orders
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isFragmentActive) return;
                    
                    recentOrders.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        order.setId(document.getId());
                        recentOrders.add(order);
                    }
                    
                    orderAdapter.notifyDataSetChanged();
                    
                    // Show message if no orders
                    if (recentOrders.isEmpty()) {
                        tvSummary.setText("You don't have any orders yet");
                    } else {
                        tvSummary.setText("Here's your store summary");
                    }
                    
                    Log.d(TAG, "Recent orders loaded: " + recentOrders.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading recent orders", e);
                    if (isFragmentActive) {
                        Toast.makeText(getContext(), "Error loading recent orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadOrderCounts() {
        if (!isFragmentActive) return;
        
        Log.d(TAG, "Loading order counts");
        
        // Do a direct count by status using separate queries for each status
        // This avoids any potential counting errors or duplicates
        loadStatusCount("PENDING", tvPendingOrderCount);
        loadStatusCount("PROCESSING", tvProcessingOrderCount);
        loadStatusCount("SHIPPED", tvShippedOrderCount);
    }
    
    private void loadStatusCount(String status, TextView countView) {
        if (!isFragmentActive) return;
        
        Log.d(TAG, "Loading count for status: " + status);
        
        firestore.collection("orders")
                .whereArrayContains("sellerIds", currentUser.getUid())
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isFragmentActive) return;
                    
                    int count = queryDocumentSnapshots.size();
                    countView.setText(String.valueOf(count));
                    
                    Log.d(TAG, "Count for " + status + ": " + count);
                    
                    // Mark as done loading after all three queries complete
                    if (status.equals("SHIPPED")) {
                        isLoadingData = false;
                        Log.d(TAG, "Loading data completed");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading " + status + " count", e);
                    
                    // Even on error, mark as done loading
                    if (status.equals("SHIPPED")) {
                        isLoadingData = false;
                        Log.d(TAG, "Loading data completed with errors");
                    }
                });
    }
    
    @Override
    public void onOrderClick(Order order) {
        // Show order details dialog
        showOrderDetailsDialog(order);
    }
    
    @Override
    public void onProcessOrderClick(Order order) {
        // Process the order based on current status
        String currentStatus = order.getStatus();
        String newStatus = "";
        String message = "";
        
        switch (currentStatus) {
            case "PENDING":
                newStatus = "PROCESSING";
                message = "Order is now being processed";
                break;
            case "PROCESSING":
                newStatus = "SHIPPED";
                message = "Order has been marked as shipped";
                break;
            case "SHIPPED":
                // Show tracking info
                showTrackingInfoDialog(order);
                return;
        }
        
        if (!newStatus.isEmpty()) {
            updateOrderStatus(order, newStatus, message);
        }
    }
    
    private void showOrderDetailsDialog(Order order) {
        if (getContext() == null) return;
        
        StringBuilder itemsText = new StringBuilder();
        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                itemsText.append(order.getItems().get(i).getTitle())
                        .append(" (x")
                        .append(order.getItems().get(i).getQuantity())
                        .append(")");
                
                if (i < order.getItems().size() - 1) {
                    itemsText.append("\n");
                }
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Order #" + (order.getOrderNumber() != null ? 
                order.getOrderNumber() : order.getId().substring(0, 8)));
        
        builder.setMessage("Status: " + order.getStatus() +
                "\nTotal: $" + String.format("%.2f", order.getTotalAmount()) +
                "\n\nItems:\n" + itemsText.toString());
        
        builder.setPositiveButton("Close", null);
        
        // Add process button if the order can be processed
        if ("PENDING".equals(order.getStatus()) || "PROCESSING".equals(order.getStatus())) {
            String actionText = "PENDING".equals(order.getStatus()) ? "Process Order" : "Ship Order";
            
            builder.setNeutralButton(actionText, (dialog, which) -> {
                onProcessOrderClick(order);
            });
        }
        
        builder.show();
    }
    
    private void showTrackingInfoDialog(Order order) {
        if (getContext() == null) return;
        
        // Get existing tracking info if any
        String trackingNumber = order.getTrackingNumber();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tracking Information");
        
        // Show existing tracking info or enter new
        if (trackingNumber != null && !trackingNumber.isEmpty()) {
            builder.setMessage("Tracking Number: " + trackingNumber);
            builder.setPositiveButton("Close", null);
        } else {
            // Create an input dialog for tracking number
            final View inputView = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_list_item_1, null);
            final TextView input = inputView.findViewById(android.R.id.text1);
            input.setHint("Enter tracking number");
            
            builder.setView(inputView);
            builder.setPositiveButton("Submit", (dialog, which) -> {
                String newTrackingNumber = input.getText().toString().trim();
                if (!newTrackingNumber.isEmpty()) {
                    updateTrackingNumber(order, newTrackingNumber);
                }
            });
            builder.setNegativeButton("Cancel", null);
        }
        
        builder.show();
    }
    
    private void updateOrderStatus(Order order, String newStatus, String message) {
        firestore.collection("orders")
                .document(order.getId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    // Update local order object
                    order.setStatus(newStatus);
                    orderAdapter.notifyDataSetChanged();
                    
                    // Refresh order counts
                    loadOrderCounts();
                    
                    // Show success message
                    if (isFragmentActive) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating order status", e);
                    if (isFragmentActive) {
                        Toast.makeText(getContext(), "Error updating order status", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void updateTrackingNumber(Order order, String trackingNumber) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("trackingNumber", trackingNumber);
        
        firestore.collection("orders")
                .document(order.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local order object
                    order.setTrackingNumber(trackingNumber);
                    orderAdapter.notifyDataSetChanged();
                    
                    // Show success message
                    if (isFragmentActive) {
                        Toast.makeText(getContext(), "Tracking information updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating tracking information", e);
                    if (isFragmentActive) {
                        Toast.makeText(getContext(), "Error updating tracking information", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
