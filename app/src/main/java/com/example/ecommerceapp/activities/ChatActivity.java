package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.MessageAdapter;
import com.example.ecommerceapp.models.Conversation;
import com.example.ecommerceapp.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    
    private RecyclerView recyclerViewMessages;
    private EditText etMessageInput;
    private Button btnSend;
    private TextView tvProductInfo;
    private Toolbar toolbar;
    
    private MessageAdapter adapter;
    private List<Message> messages;
    
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    
    private String conversationId;
    private String otherUserName;
    private String productId;
    private String productTitle;
    private String currentUserRole;
    private String otherUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view messages", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get intent data
        conversationId = getIntent().getStringExtra("conversationId");
        otherUserName = getIntent().getStringExtra("otherUserName");
        productId = getIntent().getStringExtra("productId");
        productTitle = getIntent().getStringExtra("productTitle");
        
        if (conversationId == null) {
            Toast.makeText(this, "Conversation not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(otherUserName);
        
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        tvProductInfo = findViewById(R.id.tvProductInfo);
        
        // Setup product info if available
        if (productTitle != null && !productTitle.isEmpty()) {
            tvProductInfo.setVisibility(View.VISIBLE);
            tvProductInfo.setText("Discussing: " + productTitle);
        } else {
            tvProductInfo.setVisibility(View.GONE);
        }
        
        // Setup RecyclerView
        messages = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        
        // Determine current user's role and set up the adapter
        determineUserRoleAndSetupAdapter();
        
        // Send message button click listener
        btnSend.setOnClickListener(v -> sendMessage());
    }
    
    private void determineUserRoleAndSetupAdapter() {
        firestore.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Conversation conversation = documentSnapshot.toObject(Conversation.class);
                        
                        // Determine if current user is buyer or seller
                        if (currentUser.getUid().equals(conversation.getBuyerId())) {
                            currentUserRole = "buyer";
                            otherUserId = conversation.getSellerId();
                        } else {
                            currentUserRole = "seller";
                            otherUserId = conversation.getBuyerId();
                        }
                        
                        // Setup adapter with current user ID
                        adapter = new MessageAdapter(this, messages, currentUser.getUid());
                        recyclerViewMessages.setAdapter(adapter);
                        
                        // Load messages
                        loadMessages();
                    } else {
                        Toast.makeText(this, "Conversation not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading conversation", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
    
    private void loadMessages() {
        firestore.collection("messages")
                .whereEqualTo("conversationId", conversationId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (queryDocumentSnapshots != null) {
                        messages.clear();
                        
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Message message = doc.toObject(Message.class);
                            message.setId(doc.getId());
                            messages.add(message);
                            
                            // Mark message as read if received by current user
                            if (!message.getSenderId().equals(currentUser.getUid()) && !message.isRead()) {
                                doc.getReference().update("read", true);
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        if (messages.size() > 0) {
                            recyclerViewMessages.smoothScrollToPosition(messages.size() - 1);
                        }
                        
                        // Update unread status in conversation
                        updateUnreadStatus();
                    }
                });
    }
    
    private void updateUnreadStatus() {
        DocumentReference conversationRef = firestore.collection("conversations").document(conversationId);
        
        if ("buyer".equals(currentUserRole)) {
            conversationRef.update("unreadBuyer", false);
        } else {
            conversationRef.update("unreadSeller", false);
        }
    }
    
    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();
        
        if (content.isEmpty()) {
            return;
        }
        
        // Create new message
        Message message = new Message(currentUser.getUid(), otherUserId, content, conversationId);
        
        // Add message to Firestore
        firestore.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // Clear input field
                    etMessageInput.setText("");
                    
                    // Update conversation with last message
                    updateConversation(content);
                })
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }
    
    private void updateConversation(String lastMessage) {
        DocumentReference conversationRef = firestore.collection("conversations").document(conversationId);
        
        // Update conversation with last message, timestamp, and unread status
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTimestamp", new Date());
        
        // Set unread flag for the other user
        if ("buyer".equals(currentUserRole)) {
            updates.put("unreadSeller", true);
        } else {
            updates.put("unreadBuyer", true);
        }
        
        conversationRef.update(updates)
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to update conversation", Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 