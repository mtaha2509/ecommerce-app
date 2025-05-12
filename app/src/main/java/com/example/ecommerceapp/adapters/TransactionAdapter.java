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
import com.example.ecommerceapp.models.ReleaseTransaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private final List<ReleaseTransaction> transactions;

    public TransactionAdapter(Context context, List<ReleaseTransaction> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReleaseTransaction transaction = transactions.get(position);
        
        // Set transaction ID
        holder.tvTransactionId.setText("Transaction #" + transaction.getId().substring(0, 8));
        
        // Set transaction date
        if (transaction.getReleaseDate() != null) {
            CharSequence dateText = DateUtils.getRelativeTimeSpanString(
                    transaction.getReleaseDate().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS
            );
            holder.tvReleaseDate.setText(dateText);
        } else {
            holder.tvReleaseDate.setText("Processing");
        }
        
        // Set transaction amount
        holder.tvAmount.setText(String.format("$%.2f", transaction.getAmount()));
        
        // Set order count
        int orderCount = transaction.getOrderIds() != null ? transaction.getOrderIds().size() : 0;
        holder.tvOrderCount.setText(orderCount + " " + (orderCount == 1 ? "order" : "orders"));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionId, tvReleaseDate, tvAmount, tvOrderCount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionId = itemView.findViewById(R.id.tvTransactionId);
            tvReleaseDate = itemView.findViewById(R.id.tvReleaseDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvOrderCount = itemView.findViewById(R.id.tvOrderCount);
        }
    }
} 