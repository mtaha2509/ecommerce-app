package com.example.ecommerceapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.ecommerceapp.R;
import com.example.ecommerceapp.activities.ProductListActivity;
import com.example.ecommerceapp.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private Context context;
    private List<Product> productList;
    private OnProductClickListener listener;



    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        
        // Load the first image if available
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            // Use Glide with rounded corners for better look
            RequestOptions requestOptions = new RequestOptions()
                    .transforms(new CenterCrop(), new RoundedCorners(12));
            
            Glide.with(context)
                    .load(product.getImageUrls().get(0))
                    .apply(requestOptions)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.imageView);
        } else {
            // Use placeholder if no image is available
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        holder.titleTextView.setText(product.getTitle());
        holder.priceTextView.setText(String.format("$%.2f", product.getPrice()));
        
        // Set rating information
        if (product.getReviewCount() > 0) {
            holder.ratingBar.setRating(product.getAverageRating());
            holder.ratingCountTextView.setText(String.format("(%d)", product.getReviewCount()));
            holder.ratingBar.setVisibility(View.VISIBLE);
            holder.ratingCountTextView.setVisibility(View.VISIBLE);
        } else {
            holder.ratingBar.setVisibility(View.INVISIBLE);
            holder.ratingCountTextView.setText("(No reviews)");
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView priceTextView;
        RatingBar ratingBar;
        TextView ratingCountTextView;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_product);
            titleTextView = itemView.findViewById(R.id.tv_title);
            priceTextView = itemView.findViewById(R.id.tv_price);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            ratingCountTextView = itemView.findViewById(R.id.tv_rating_count);
        }
    }
} 