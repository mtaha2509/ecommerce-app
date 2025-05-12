package com.example.ecommerceapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment implements OrderAdapter.OrderClickListener {
    private static final String TAG = "OrdersFragment";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private OrderAdapter adapter;
    private List<Order> orderList;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to view orders", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewOrders);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Initialize order list
        orderList = new ArrayList<>();
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(getContext(), orderList, this);
        recyclerView.setAdapter(adapter);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadOrders);
        
        // Load orders
        loadOrders();
    }
    
    private void loadOrders() {
        if (currentUser == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        firestore.collection("orders")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        order.setId(document.getId());
                        orderList.add(order);
                    }
                    
                    adapter.notifyDataSetChanged();
                    
                    if (orderList.isEmpty()) {
                        showEmptyView("You haven't placed any orders yet");
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders", e);
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
    
    @Override
    public void onOrderClick(Order order) {
        // Show order details dialog
        showOrderDetailsDialog(order);
    }
    
    @Override
    public void onOrderReceived(Order order) {
        if (getContext() == null) return;
        
        // Confirm with the user
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Receipt")
                .setMessage("Did you receive this order? This will mark the order as COMPLETED.")
                .setPositiveButton("Yes, I received it", (dialog, which) -> {
                    updateOrderStatus(order, "COMPLETED");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateOrderStatus(Order order, String newStatus) {
        if (getContext() == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        
        firestore.collection("orders")
                .document(order.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local data
                    order.setStatus(newStatus);
                    adapter.notifyDataSetChanged();
                    
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Order marked as " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error updating order status", e);
                    Toast.makeText(getContext(), "Error updating order status", Toast.LENGTH_SHORT).show();
                });
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
        
        // If order is shipped, add a button to mark as received
        if ("SHIPPED".equals(order.getStatus())) {
            builder.setNeutralButton("Mark as Received", (dialog, which) -> {
                onOrderReceived(order);
            });
        }
        
        builder.show();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadOrders();
        }
    }
} 