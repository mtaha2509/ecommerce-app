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
import com.example.ecommerceapp.adapters.TransactionAdapter;
import com.example.ecommerceapp.models.ReleaseTransaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReleasedIncomeFragment extends Fragment {
    private static final String TAG = "ReleasedIncomeFragment";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView, tvTotalReleased;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private TransactionAdapter adapter;
    private List<ReleaseTransaction> transactionList;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    private double totalReleased = 0.0;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_released_income, container, false);
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
        recyclerView = view.findViewById(R.id.recyclerViewTransactions);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvTotalReleased = view.findViewById(R.id.tvTotalReleased);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Initialize transaction list
        transactionList = new ArrayList<>();
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(getContext(), transactionList);
        recyclerView.setAdapter(adapter);
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadTransactions);
        
        // Load transactions
        loadTransactions();
    }
    
    private void loadTransactions() {
        if (currentUser == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        // Reset total amount
        totalReleased = 0.0;
        
        // Clear transactions list to avoid duplicates
        transactionList.clear();
        
        firestore.collection("releaseTransactions")
                .whereEqualTo("sellerId", currentUser.getUid())
                .orderBy("releaseDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ReleaseTransaction transaction = document.toObject(ReleaseTransaction.class);
                        transaction.setId(document.getId());
                        transactionList.add(transaction);
                        
                        // Calculate total released
                        totalReleased += transaction.getAmount();
                    }
                    
                    // Update total amount text
                    tvTotalReleased.setText(String.format("Total Released: $%.2f", totalReleased));
                    
                    adapter.notifyDataSetChanged();
                    
                    if (transactionList.isEmpty()) {
                        showEmptyView("No release history yet");
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions", e);
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    showEmptyView("Error loading transactions");
                });
    }
    
    private void showEmptyView(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Remove redundant loading call to prevent duplication
        // No need to call loadTransactions() again since it's already called in onViewCreated
    }
} 