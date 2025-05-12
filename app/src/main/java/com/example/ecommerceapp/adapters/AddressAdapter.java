package com.example.ecommerceapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Address;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {
    
    private Context context;
    private List<Address> addressList;
    private AddressClickListener listener;
    
    public interface AddressClickListener {
        void onAddressEdit(int position);
        void onAddressDelete(int position);
        void onSetDefault(int position);
    }
    
    public AddressAdapter(Context context, List<Address> addressList, AddressClickListener listener) {
        this.context = context;
        this.addressList = addressList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        Address address = addressList.get(position);
        
        holder.tvAddressName.setText(address.getName());
        holder.tvRecipientName.setText(address.getRecipientName());
        holder.tvAddressDetails.setText(address.getFormattedAddress());
        holder.tvPhoneNumber.setText(address.getPhoneNumber());
        
        // Handle default address
        if (address.isDefault()) {
            holder.tvDefault.setVisibility(View.VISIBLE);
            holder.btnSetDefault.setVisibility(View.GONE);
        } else {
            holder.tvDefault.setVisibility(View.GONE);
            holder.btnSetDefault.setVisibility(View.VISIBLE);
        }
        
        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddressEdit(position);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddressDelete(position);
            }
        });
        
        holder.btnSetDefault.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetDefault(position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return addressList.size();
    }
    
    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvAddressName, tvRecipientName, tvAddressDetails, tvPhoneNumber, tvDefault;
        ImageButton btnEdit, btnDelete, btnSetDefault;
        
        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardView);
            tvAddressName = itemView.findViewById(R.id.tvAddressName);
            tvRecipientName = itemView.findViewById(R.id.tvRecipientName);
            tvAddressDetails = itemView.findViewById(R.id.tvAddressDetails);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvDefault = itemView.findViewById(R.id.tvDefault);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnSetDefault = itemView.findViewById(R.id.btnSetDefault);
        }
    }
} 