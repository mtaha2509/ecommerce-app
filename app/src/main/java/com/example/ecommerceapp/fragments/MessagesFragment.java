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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.activities.ChatActivity;
import com.example.ecommerceapp.adapters.ConversationAdapter;
import com.example.ecommerceapp.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment implements ConversationAdapter.ConversationClickListener {
    private static final String TAG = "MessagesFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    
    private ConversationAdapter adapter;
    private List<Conversation> conversations;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String userRole = "";
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to view messages", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewConversations);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        
        // Setup RecyclerView
        conversations = new ArrayList<>();
        adapter = new ConversationAdapter(getContext(), conversations, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadConversations);
        
        // Determine user role
        determineUserRole();
    }
    
    private void determineUserRole() {
        progressBar.setVisibility(View.VISIBLE);
        
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                        userRole = documentSnapshot.getString("role");
                        loadConversations();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        showEmptyView("User role not found");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showEmptyView("Error loading data");
                    Log.e(TAG, "Error determining user role", e);
                });
    }
    
    private void loadConversations() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Clear conversations at the beginning to avoid duplicates
        conversations.clear();
        
        // Query conversations based on user role
        Query query;
        if ("seller".equals(userRole)) {
            query = firestore.collection("conversations")
                    .whereEqualTo("sellerId", currentUser.getUid())
                    .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);
        } else {
            // Assume buyer role by default
            query = firestore.collection("conversations")
                    .whereEqualTo("buyerId", currentUser.getUid())
                    .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);
        }
        
        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // No need to clear here since we've already cleared at the beginning
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Conversation conversation = document.toObject(Conversation.class);
                        conversation.setId(document.getId());
                        conversations.add(conversation);
                    }
                    
                    updateUI();
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    showEmptyView("Error loading conversations");
                    Log.e(TAG, "Error loading conversations", e);
                });
    }
    
    private void updateUI() {
        if (conversations.isEmpty()) {
            showEmptyView("No conversations yet");
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }
    
    private void showEmptyView(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("conversationId", conversation.getId());
        intent.putExtra("otherUserName", "seller".equals(userRole) ? conversation.getBuyerName() : conversation.getSellerName());
        if (conversation.getProductId() != null) {
            intent.putExtra("productId", conversation.getProductId());
            intent.putExtra("productTitle", conversation.getProductTitle());
        }
        
        // Mark messages as read
        boolean isUnread = "seller".equals(userRole) ? conversation.isUnreadSeller() : conversation.isUnreadBuyer();
        if (isUnread) {
            if ("seller".equals(userRole)) {
                firestore.collection("conversations").document(conversation.getId())
                        .update("unreadSeller", false);
            } else {
                firestore.collection("conversations").document(conversation.getId())
                        .update("unreadBuyer", false);
            }
        }
        
        startActivity(intent);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Remove redundant loading call to prevent duplication
        // No need to call loadConversations() again since it's called in determineUserRole()
    }
}
