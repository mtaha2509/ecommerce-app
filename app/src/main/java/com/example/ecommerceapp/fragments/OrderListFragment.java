package com.example.ecommerceapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.SellerOrderAdapter;
import com.example.ecommerceapp.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderListFragment extends Fragment implements SellerOrderAdapter.SellerOrderClickListener {
    private static final String TAG = "OrderListFragment";
    private static final String ARG_STATUS = "status";
    private static final String ARG_START_DATE = "start_date";
    private static final String ARG_END_DATE = "end_date";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private String statusFilter;
    private Date startDate;
    private Date endDate;
    
    private List<Order> orderList;
    private SellerOrderAdapter adapter;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    public static OrderListFragment newInstance(String status, Date startDate, Date endDate) {
        OrderListFragment fragment = new OrderListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        
        if (startDate != null) {
            args.putLong(ARG_START_DATE, startDate.getTime());
        }
        
        if (endDate != null) {
            args.putLong(ARG_END_DATE, endDate.getTime());
        }
        
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            statusFilter = getArguments().getString(ARG_STATUS);
            
            if (getArguments().containsKey(ARG_START_DATE)) {
                startDate = new Date(getArguments().getLong(ARG_START_DATE));
            }
            
            if (getArguments().containsKey(ARG_END_DATE)) {
                endDate = new Date(getArguments().getLong(ARG_END_DATE));
            }
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_list, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            return;
        }
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewOrders);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyView = view.findViewById(R.id.tvEmptyView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Set up RecyclerView
        orderList = new ArrayList<>();
        adapter = new SellerOrderAdapter(getContext(), orderList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadOrders);
        
        // Load orders
        loadOrders();
    }
    
    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyView.setVisibility(View.GONE);
        
        // Start with base query
        Query query = firestore.collection("orders")
                .whereArrayContains("sellerIds", currentUser.getUid());
        
        // Add status filter
        if (statusFilter != null && !statusFilter.equals("ALL")) {
            query = query.whereEqualTo("status", statusFilter);
        }
        
        // Add date filters if set
        if (startDate != null) {
            query = query.whereGreaterThanOrEqualTo("orderDate", startDate);
        }
        
        if (endDate != null) {
            query = query.whereLessThanOrEqualTo("orderDate", endDate);
        }
        
        // Execute query
        query.orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        order.setId(document.getId());
                        orderList.add(order);
                    }
                    
                    updateUI();
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
    
    private void updateUI() {
        if (orderList.isEmpty()) {
            showEmptyView("No orders found");
        } else {
            tvEmptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
    
    private void showEmptyView(String message) {
        tvEmptyView.setText(message);
        tvEmptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    @Override
    public void onOrderClick(Order order) {
        // Show order details dialog
        if (getActivity() instanceof SellerOrderAdapter.SellerOrderClickListener) {
            ((SellerOrderAdapter.SellerOrderClickListener) getActivity()).onOrderClick(order);
        }
    }
    
    @Override
    public void onProcessOrderClick(Order order) {
        // Process the order
        if (getActivity() instanceof SellerOrderAdapter.SellerOrderClickListener) {
            ((SellerOrderAdapter.SellerOrderClickListener) getActivity()).onProcessOrderClick(order);
            
            // Refresh the list to reflect any changes
            loadOrders();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadOrders();
        }
    }
} 