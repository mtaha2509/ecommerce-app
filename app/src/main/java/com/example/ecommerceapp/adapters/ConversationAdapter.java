package com.example.ecommerceapp.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    
    private final Context context;
    private final List<Conversation> conversations;
    private final ConversationClickListener listener;
    private final String currentUserId;
    
    public ConversationAdapter(Context context, List<Conversation> conversations, ConversationClickListener listener) {
        this.context = context;
        this.conversations = conversations;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        
        // Determine if user is buyer or seller
        boolean isSeller = currentUserId.equals(conversation.getSellerId());
        
        // Set user name (show the other party's name)
        String otherUserName = isSeller ? conversation.getBuyerName() : conversation.getSellerName();
        holder.tvUserName.setText(otherUserName);
        
        // Set last message
        if (conversation.getLastMessage() != null && !conversation.getLastMessage().isEmpty()) {
            holder.tvLastMessage.setText(conversation.getLastMessage());
        } else {
            holder.tvLastMessage.setText("No messages yet");
        }
        
        // Set timestamp
        if (conversation.getLastMessageTimestamp() != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    conversation.getLastMessageTimestamp().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            holder.tvTimestamp.setText(timeAgo);
        } else {
            holder.tvTimestamp.setText("");
        }
        
        // Show product info if available
        if (conversation.getProductTitle() != null && !conversation.getProductTitle().isEmpty()) {
            holder.tvProductTitle.setVisibility(View.VISIBLE);
            holder.tvProductTitle.setText("Product: " + conversation.getProductTitle());
        } else {
            holder.tvProductTitle.setVisibility(View.GONE);
        }
        
        // Show unread indicator
        boolean hasUnread = isSeller ? conversation.isUnreadSeller() : conversation.isUnreadBuyer();
        holder.ivUnread.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return conversations.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvLastMessage, tvTimestamp, tvProductTitle;
        ImageView ivUnread;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvProductTitle = itemView.findViewById(R.id.tvProductTitle);
            ivUnread = itemView.findViewById(R.id.ivUnread);
        }
    }
    
    public interface ConversationClickListener {
        void onConversationClick(Conversation conversation);
    }
} 