package com.example.ecommerceapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.activities.CartActivity;
import com.example.ecommerceapp.activities.ProductDetailActivity;
import com.example.ecommerceapp.adapters.ProductAdapter;
import com.example.ecommerceapp.models.Product;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductsFragment extends Fragment implements ProductAdapter.OnProductClickListener {
    private static final String TAG = "ProductsFragment";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private ChipGroup categoryChipGroup;
    private FloatingActionButton fabCart;
    private RangeSlider priceRangeSlider;
    
    private ProductAdapter adapter;
    private List<Product> productList;
    private List<Product> filteredList;
    private FirebaseFirestore firestore;
    
    // Filter variables
    private String currentCategory = "";
    private float minPrice = 0;
    private float maxPrice = Float.MAX_VALUE;
    private String sortBy = "timestamp";
    private boolean sortAscending = false;
    private Set<String> categoriesList = new HashSet<>();
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        firestore = FirebaseFirestore.getInstance();
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewProducts);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        searchView = view.findViewById(R.id.searchView);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        fabCart = view.findViewById(R.id.fabCart);
        priceRangeSlider = view.findViewById(R.id.priceRangeSlider);
        
        // Initialize lists
        productList = new ArrayList<>();
        filteredList = new ArrayList<>();
        
        // Setup RecyclerView with Grid Layout (2 columns)
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ProductAdapter(getContext(), filteredList);
        adapter.setOnProductClickListener(this);
        recyclerView.setAdapter(adapter);
        
        // Setup pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadProducts);
        
        // Setup search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProducts(query);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                filterProducts(newText);
                return true;
            }
        });
        
        // Setup cart button
        fabCart.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CartActivity.class);
            startActivity(intent);
        });
        
        // Setup price range slider
        priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                minPrice = slider.getValues().get(0);
                maxPrice = slider.getValues().get(1);
                applyFilters();
            }
        });
        
        // Load products
        loadProducts();
    }
    
    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        firestore.collection("products")
                .orderBy(sortBy, sortAscending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    categoriesList.clear();
                    
                    float maxPriceValue = 0;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                        
                        // Collect categories for filter
                        if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                            categoriesList.add(product.getCategory());
                        }
                        
                        // Track maximum price for slider
                        if (product.getPrice() > maxPriceValue) {
                            maxPriceValue = (float) product.getPrice();
                        }
                    }
                    
                    // Setup price range slider values
                    priceRangeSlider.setValueFrom(0);
                    priceRangeSlider.setValueTo(maxPriceValue > 0 ? maxPriceValue : 1000);
                    priceRangeSlider.setValues(0f, maxPriceValue > 0 ? maxPriceValue : 1000);
                    
                    // Setup category chips
                    setupCategoryChips();
                    
                    // Apply initial filters
                    applyFilters();
                    
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading products", e);
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    showEmptyView("Error loading products");
                });
    }
    
    private void setupCategoryChips() {
        categoryChipGroup.removeAllViews();
        
        // Add "All" chip
        Chip allChip = new Chip(getContext());
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked(currentCategory.isEmpty());
        allChip.setOnClickListener(v -> {
            currentCategory = "";
            applyFilters();
        });
        categoryChipGroup.addView(allChip);
        
        // Add category chips
        for (String category : categoriesList) {
            Chip chip = new Chip(getContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(category.equals(currentCategory));
            
            chip.setOnClickListener(v -> {
                currentCategory = category;
                applyFilters();
            });
            
            categoryChipGroup.addView(chip);
        }
    }
    
    private void applyFilters() {
        filteredList.clear();
        
        for (Product product : productList) {
            // Apply category filter
            boolean categoryMatch = currentCategory.isEmpty() || 
                    (product.getCategory() != null && product.getCategory().equals(currentCategory));
            
            // Apply price filter
            boolean priceMatch = product.getPrice() >= minPrice && product.getPrice() <= maxPrice;
            
            if (categoryMatch && priceMatch) {
                filteredList.add(product);
            }
        }
        
        if (filteredList.isEmpty()) {
            showEmptyView("No products match your filters");
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private void filterProducts(String query) {
        if (query.isEmpty()) {
            applyFilters();
            return;
        }
        
        List<Product> searchFilteredList = new ArrayList<>();
        String lowercaseQuery = query.toLowerCase();
        
        for (Product product : filteredList) {
            boolean titleMatch = product.getTitle() != null && 
                    product.getTitle().toLowerCase().contains(lowercaseQuery);
            boolean descriptionMatch = product.getDescription() != null && 
                    product.getDescription().toLowerCase().contains(lowercaseQuery);
            boolean categoryMatch = product.getCategory() != null && 
                    product.getCategory().toLowerCase().contains(lowercaseQuery);
            
            if (titleMatch || descriptionMatch || categoryMatch) {
                searchFilteredList.add(product);
            }
        }
        
        filteredList.clear();
        filteredList.addAll(searchFilteredList);
        
        if (filteredList.isEmpty()) {
            showEmptyView("No products match your search");
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private void showEmptyView(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
        intent.putExtra("productId", product.getId());
        startActivity(intent);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadProducts(); // Reload products when returning to fragment
    }
} 