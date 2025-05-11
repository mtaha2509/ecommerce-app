package com.example.ecommerceapp.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> orders;
    private final OrderClickListener listener;

    public OrderAdapter(Context context, List<Order> orders, OrderClickListener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        
        // Set order ID or number
        holder.tvOrderId.setText("Order #" + (order.getOrderNumber() != null ? 
                order.getOrderNumber() : order.getId().substring(0, 8)));
        
        // Set order date
        if (order.getOrderDate() != null) {
            CharSequence dateText = DateUtils.getRelativeTimeSpanString(
                    order.getOrderDate().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS
            );
            holder.tvOrderDate.setText(dateText);
        } else {
            holder.tvOrderDate.setText("Processing");
        }
        
        // Set order status
        holder.tvOrderStatus.setText(order.getStatus());
        
        // Set order amount
        holder.tvOrderAmount.setText(String.format("$%.2f", order.getTotalAmount()));
        
        // Set item count
        int itemCount = order.getItems() != null ? order.getItems().size() : 0;
        holder.tvItemCount.setText(itemCount + " " + (itemCount == 1 ? "item" : "items"));
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderStatus, tvOrderAmount, tvItemCount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
        }
    }

    public interface OrderClickListener {
        void onOrderClick(Order order);
    }
} 