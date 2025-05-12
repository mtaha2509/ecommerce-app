package com.example.ecommerceapp.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Order;

import java.util.List;

public class SellerOrderAdapter extends RecyclerView.Adapter<SellerOrderAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> orders;
    private final SellerOrderClickListener listener;

    public SellerOrderAdapter(Context context, List<Order> orders, SellerOrderClickListener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_seller_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        
        // Set order number/ID
        String orderIdentifier = order.getOrderNumber() != null ? 
                order.getOrderNumber() : order.getId().substring(0, 8);
        holder.tvOrderNumber.setText("Order #" + orderIdentifier);
        
        // Set order date
        if (order.getOrderDate() != null) {
            CharSequence dateText = DateUtils.getRelativeTimeSpanString(
                    order.getOrderDate().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS
            );
            holder.tvOrderDate.setText(dateText);
        } else {
            holder.tvOrderDate.setText("Recently");
        }
        
        // Set number of items
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        holder.tvOrderItems.setText(itemCount + " " + (itemCount == 1 ? "item" : "items"));
        
        // Set order amount
        holder.tvOrderAmount.setText(String.format("$%.2f", order.getTotalAmount()));
        
        // Set order status with appropriate color
        String status = order.getStatus();
        holder.tvOrderStatus.setText(status);
        
        // Set status color
        int statusColor;
        switch (status) {
            case "PENDING":
                statusColor = 0xFFFF9800; // Orange
                holder.btnProcessOrder.setText("Process");
                holder.btnProcessOrder.setVisibility(View.VISIBLE);
                break;
            case "PROCESSING":
                statusColor = 0xFF2196F3; // Blue
                holder.btnProcessOrder.setText("Ship");
                holder.btnProcessOrder.setVisibility(View.VISIBLE);
                break;
            case "SHIPPED":
                statusColor = 0xFF4CAF50; // Green
                holder.btnProcessOrder.setText("Track");
                holder.btnProcessOrder.setVisibility(View.VISIBLE);
                break;
            case "DELIVERED":
                statusColor = 0xFF4CAF50; // Green
                holder.btnProcessOrder.setVisibility(View.GONE);
                break;
            case "CANCELLED":
                statusColor = 0xFFF44336; // Red
                holder.btnProcessOrder.setVisibility(View.GONE);
                break;
            default:
                statusColor = 0xFF9E9E9E; // Grey
                holder.btnProcessOrder.setVisibility(View.GONE);
                break;
        }
        
        holder.tvOrderStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
        
        holder.btnProcessOrder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProcessOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(orders.size(), 5); // Only show up to 5 recent orders
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNumber, tvOrderDate, tvOrderItems, tvOrderAmount, tvOrderStatus;
        Button btnProcessOrder;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNumber = itemView.findViewById(R.id.tvOrderNumber);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            btnProcessOrder = itemView.findViewById(R.id.btnProcessOrder);
        }
    }

    public interface SellerOrderClickListener {
        void onOrderClick(Order order);
        void onProcessOrderClick(Order order);
    }
} 