package com.example.ecommerceapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.CartItem;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    
    private final Context context;
    private final List<CartItem> cartItems;
    private final CartItemListener listener;
    
    public interface CartItemListener {
        void onQuantityChange(CartItem item, int newQuantity);
        void onItemRemove(CartItem item);
    }
    
    public CartAdapter(Context context, List<CartItem> cartItems, CartItemListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        
        holder.tvTitle.setText(item.getTitle());
        holder.tvPrice.setText(String.format("$%.2f", item.getPrice()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvItemTotal.setText(String.format("$%.2f", item.getPrice() * item.getQuantity()));
        
        // Load product image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.ivProduct);
        }
        
        // Set click listeners
        holder.btnIncrease.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            listener.onQuantityChange(item, newQuantity);
        });
        
        holder.btnDecrease.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() - 1;
            listener.onQuantityChange(item, newQuantity);
        });
        
        holder.btnRemove.setOnClickListener(v -> listener.onItemRemove(item));
    }
    
    @Override
    public int getItemCount() {
        return cartItems.size();
    }
    
    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvTitle, tvPrice, tvQuantity, tvItemTotal;
        ImageButton btnIncrease, btnDecrease, btnRemove;
        
        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            
            ivProduct = itemView.findViewById(R.id.ivCartProduct);
            tvTitle = itemView.findViewById(R.id.tvCartItemTitle);
            tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartItemQuantity);
            tvItemTotal = itemView.findViewById(R.id.tvCartItemTotal);
            btnIncrease = itemView.findViewById(R.id.btnCartIncrease);
            btnDecrease = itemView.findViewById(R.id.btnCartDecrease);
            btnRemove = itemView.findViewById(R.id.btnCartRemove);
        }
    }
} 