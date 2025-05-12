package com.example.ecommerceapp.fragments;

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.OrderAdapter;
import com.example.ecommerceapp.models.Order;
import com.example.ecommerceapp.models.ReleaseTransaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToReleaseIncomeFragment extends Fragment implements OrderAdapter.OrderClickListener {
    private static final String TAG = "ToReleaseIncomeFragment";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView, tvTotalAmount;
    private Button btnRedeem;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private OrderAdapter adapter;
    private List<Order> completedOrders;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    private double totalAmount = 0.0;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_to_release_income, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewOrders);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        btnRedeem = view.findViewById(R.id.btnRedeem);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Initialize order list
        completedOrders = new ArrayList<>();
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(getContext(), completedOrders, this);
        recyclerView.setAdapter(adapter);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadCompletedOrders);
        
        // Set up Redeem button
        btnRedeem.setOnClickListener(v -> redeemFunds());
        
        // Load orders
        loadCompletedOrders();
    }
    
    private void loadCompletedOrders() {
        if (currentUser == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        // Reset total amount
        totalAmount = 0.0;
        
        Log.d(TAG, "Loading completed orders for seller: " + currentUser.getUid());
        
        // Clear previous orders first to avoid duplicates
        completedOrders.clear();
        
        // First, get all orders for this seller
        firestore.collection("orders")
                .whereArrayContains("sellerIds", currentUser.getUid())
                .get()
                .addOnSuccessListener(allOrdersSnapshot -> {
                    Log.d(TAG, "Total orders found for seller: " + allOrdersSnapshot.size());
                    
                    // Now filter for completed non-redeemed orders
                    firestore.collection("orders")
                            .whereArrayContains("sellerIds", currentUser.getUid())
                            .whereEqualTo("status", "COMPLETED")
                            .get()
                            .addOnSuccessListener(completedSnapshot -> {
                                Log.d(TAG, "Completed orders found: " + completedSnapshot.size());
                                
                                // No need to clear here since we already cleared at the beginning
                                
                                for (QueryDocumentSnapshot document : completedSnapshot) {
                                    Order order = document.toObject(Order.class);
                                    order.setId(document.getId());
                                    
                                    // Check if redeemed field exists and is false
                                    Boolean redeemedValue = document.getBoolean("redeemed");
                                    Log.d(TAG, "Order " + order.getId() + " redeemed value: " + redeemedValue);
                                    
                                    if (redeemedValue == null || !redeemedValue) {
                                        completedOrders.add(order);
                                        
                                        // Calculate total amount (in a real app, you'd calculate the seller's portion)
                                        totalAmount += order.getTotalAmount();
                                    }
                                }
                                
                                // Update total amount text
                                tvTotalAmount.setText(String.format("Total to Release: $%.2f", totalAmount));
                                
                                // Enable/disable redeem button based on whether there are orders
                                btnRedeem.setEnabled(!completedOrders.isEmpty());
                                
                                adapter.notifyDataSetChanged();
                                
                                if (completedOrders.isEmpty()) {
                                    showEmptyView("No funds to release at this time");
                                } else {
                                    emptyView.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                }
                                
                                progressBar.setVisibility(View.GONE);
                                swipeRefreshLayout.setRefreshing(false);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading completed orders", e);
                                progressBar.setVisibility(View.GONE);
                                swipeRefreshLayout.setRefreshing(false);
                                showEmptyView("Error loading orders");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking all orders", e);
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    showEmptyView("Error loading orders");
                });
    }
    
    private void showEmptyView(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    private void redeemFunds() {
        if (getContext() == null || completedOrders.isEmpty()) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle("Redeem Funds")
                .setMessage("Do you want to redeem $" + String.format("%.2f", totalAmount) + " to your account?")
                .setPositiveButton("Redeem", (dialog, which) -> {
                    processRedemption();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void processRedemption() {
        if (completedOrders.isEmpty()) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Create a release transaction record
        ReleaseTransaction transaction = new ReleaseTransaction();
        transaction.setSellerId(currentUser.getUid());
        transaction.setAmount(totalAmount);
        transaction.setReleaseDate(new Date());
        
        // Create a list of order IDs for reference
        List<String> orderIds = new ArrayList<>();
        for (Order order : completedOrders) {
            orderIds.add(order.getId());
        }
        transaction.setOrderIds(orderIds);
        
        // Save the transaction to Firestore
        firestore.collection("releaseTransactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    // Mark all orders as redeemed
                    markOrdersAsRedeemed(orderIds);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error creating release transaction", e);
                    Toast.makeText(getContext(), "Error redeeming funds", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void markOrdersAsRedeemed(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Counter to track completed updates
        final int[] completedUpdates = {0};
        final int totalUpdates = orderIds.size();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("redeemed", true);
        
        for (String orderId : orderIds) {
            firestore.collection("orders")
                    .document(orderId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        completedUpdates[0]++;
                        if (completedUpdates[0] >= totalUpdates) {
                            // All orders updated
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Funds redeemed successfully", Toast.LENGTH_SHORT).show();
                            loadCompletedOrders(); // Refresh the list
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedUpdates[0]++;
                        Log.e(TAG, "Error updating order " + orderId, e);
                        
                        if (completedUpdates[0] >= totalUpdates) {
                            // All attempts completed, even with some failures
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Some orders could not be updated", Toast.LENGTH_SHORT).show();
                            loadCompletedOrders(); // Refresh the list
                        }
                    });
        }
    }
    
    @Override
    public void onOrderClick(Order order) {
        // Show order details
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
        
        new AlertDialog.Builder(getContext())
                .setTitle("Order #" + (order.getOrderNumber() != null ? 
                        order.getOrderNumber() : order.getId().substring(0, 8)))
                .setMessage("Status: " + order.getStatus() +
                        "\nTotal: $" + String.format("%.2f", order.getTotalAmount()) +
                        "\n\nItems:\n" + itemsText.toString())
                .setPositiveButton("Close", null)
                .show();
    }
    
    @Override
    public void onOrderReceived(Order order) {
        // Not used in this fragment
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Remove redundant loading call to prevent duplication
        // No need to call loadCompletedOrders() again since it's already called in onViewCreated
    }
} 