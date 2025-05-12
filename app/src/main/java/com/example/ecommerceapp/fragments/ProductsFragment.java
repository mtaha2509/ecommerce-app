package com.example.ecommerceapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
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
    private TextView btnApplyFilters;
    private TextView tvSortBy;
    private ChipGroup activeFiltersChipGroup;
    
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
    private String currentQuery = "";
    private String sortLabel = "Newest First";
    private boolean hasActiveFilters = false;
    
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
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters);
        tvSortBy = view.findViewById(R.id.tvSortBy);
        activeFiltersChipGroup = view.findViewById(R.id.activeFiltersChipGroup);
        
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
                currentQuery = query;
                filterProducts();
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                filterProducts();
                return true;
            }
        });
        
        // Setup filter button
        btnApplyFilters.setOnClickListener(v -> showFilterDialog());
        
        // Setup sort button
        tvSortBy.setOnClickListener(v -> showSortDialog());
        
        // Setup cart button
        fabCart.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CartActivity.class);
            startActivity(intent);
        });
        
        // Load products
        loadProducts();
    }
    
    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_products, null);
        
        TextInputEditText etMinPrice = dialogView.findViewById(R.id.etMinPrice);
        TextInputEditText etMaxPrice = dialogView.findViewById(R.id.etMaxPrice);
        AutoCompleteTextView spinnerSortBy = dialogView.findViewById(R.id.spinnerSortBy);
        MaterialButton btnReset = dialogView.findViewById(R.id.btnReset);
        MaterialButton btnApply = dialogView.findViewById(R.id.btnApply);
        
        // Set current values
        if (minPrice > 0) {
            etMinPrice.setText(String.valueOf(minPrice));
        }
        
        if (maxPrice < Float.MAX_VALUE) {
            etMaxPrice.setText(String.valueOf(maxPrice));
        }
        
        // Setup sort options dropdown
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sort_options, android.R.layout.simple_dropdown_item_1line);
        spinnerSortBy.setAdapter(adapter);
        spinnerSortBy.setText(sortLabel, false);
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        
        // Reset button click
        btnReset.setOnClickListener(v -> {
            etMinPrice.setText("");
            etMaxPrice.setText("");
            spinnerSortBy.setText(adapter.getItem(0), false);
        });
        
        // Apply button click
        btnApply.setOnClickListener(v -> {
            // Get min price
            String minPriceStr = etMinPrice.getText().toString().trim();
            if (!TextUtils.isEmpty(minPriceStr)) {
                try {
                    minPrice = Float.parseFloat(minPriceStr);
                } catch (NumberFormatException e) {
                    minPrice = 0;
                }
            } else {
                minPrice = 0;
            }
            
            // Get max price
            String maxPriceStr = etMaxPrice.getText().toString().trim();
            if (!TextUtils.isEmpty(maxPriceStr)) {
                try {
                    maxPrice = Float.parseFloat(maxPriceStr);
                } catch (NumberFormatException e) {
                    maxPrice = Float.MAX_VALUE;
                }
            } else {
                maxPrice = Float.MAX_VALUE;
            }
            
            // Get sort option
            String selectedSort = spinnerSortBy.getText().toString();
            sortLabel = selectedSort;
            
            if (selectedSort.equals("Newest First")) {
                sortBy = "timestamp";
                sortAscending = false;
            } else if (selectedSort.equals("Price: Low to High")) {
                sortBy = "price";
                sortAscending = true;
            } else if (selectedSort.equals("Price: High to Low")) {
                sortBy = "price";
                sortAscending = false;
            } else if (selectedSort.equals("Rating: High to Low")) {
                sortBy = "averageRating";
                sortAscending = false;
            }
            
            // Apply filters
            updateActiveFiltersChips();
            filterProducts();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sort By");
        
        String[] sortOptions = getResources().getStringArray(R.array.sort_options);
        
        // Find the current sort option index
        int selectedIndex = 0;
        for (int i = 0; i < sortOptions.length; i++) {
            if (sortOptions[i].equals(sortLabel)) {
                selectedIndex = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(sortOptions, selectedIndex, (dialog, which) -> {
            String selectedSort = sortOptions[which];
            sortLabel = selectedSort;
            
            if (selectedSort.equals("Newest First")) {
                sortBy = "timestamp";
                sortAscending = false;
            } else if (selectedSort.equals("Price: Low to High")) {
                sortBy = "price";
                sortAscending = true;
            } else if (selectedSort.equals("Price: High to Low")) {
                sortBy = "price";
                sortAscending = false;
            } else if (selectedSort.equals("Rating: High to Low")) {
                sortBy = "averageRating";
                sortAscending = false;
            }
            
            updateActiveFiltersChips();
            filterProducts();
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    private void updateActiveFiltersChips() {
        activeFiltersChipGroup.removeAllViews();
        hasActiveFilters = false;
        
        // Add price filter chip if applicable
        if (minPrice > 0 || maxPrice < Float.MAX_VALUE) {
            hasActiveFilters = true;
            
            String priceLabel;
            if (minPrice > 0 && maxPrice < Float.MAX_VALUE) {
                priceLabel = "$" + minPrice + " - $" + maxPrice;
            } else if (minPrice > 0) {
                priceLabel = "$" + minPrice + " & up";
            } else {
                priceLabel = "Up to $" + maxPrice;
            }
            
            Chip priceChip = new Chip(requireContext());
            priceChip.setText(priceLabel);
            priceChip.setCloseIconVisible(true);
            priceChip.setOnCloseIconClickListener(v -> {
                minPrice = 0;
                maxPrice = Float.MAX_VALUE;
                updateActiveFiltersChips();
                filterProducts();
            });
            activeFiltersChipGroup.addView(priceChip);
        }
        
        // Add sort chip
        if (!sortLabel.equals("Newest First")) {
            hasActiveFilters = true;
            
            Chip sortChip = new Chip(requireContext());
            sortChip.setText(sortLabel);
            sortChip.setCloseIconVisible(true);
            sortChip.setOnCloseIconClickListener(v -> {
                sortBy = "timestamp";
                sortAscending = false;
                sortLabel = "Newest First";
                updateActiveFiltersChips();
                filterProducts();
            });
            activeFiltersChipGroup.addView(sortChip);
        }
        
        activeFiltersChipGroup.setVisibility(hasActiveFilters ? View.VISIBLE : View.GONE);
    }
    
    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        firestore.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    categoriesList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                        
                        // Collect categories for filter
                        if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                            categoriesList.add(product.getCategory());
                        }
                    }
                    
                    // Setup category chips
                    setupCategoryChips();
                    
                    // Apply filters
                    filterProducts();
                    
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
        allChip.setChipBackgroundColorResource(R.color.chip_background);
        allChip.setOnClickListener(v -> {
            currentCategory = "";
            filterProducts();
        });
        categoryChipGroup.addView(allChip);
        
        // Add category chips
        for (String category : categoriesList) {
            Chip chip = new Chip(getContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(category.equals(currentCategory));
            chip.setChipBackgroundColorResource(R.color.chip_background);
            
            chip.setOnClickListener(v -> {
                currentCategory = category;
                filterProducts();
            });
            
            categoryChipGroup.addView(chip);
        }
    }
    
    private void filterProducts() {
        filteredList.clear();
        
        for (Product product : productList) {
            // Apply category filter
            boolean categoryMatch = currentCategory.isEmpty() || 
                    (product.getCategory() != null && product.getCategory().equals(currentCategory));
            
            // Apply price filter
            boolean priceMatch = product.getPrice() >= minPrice && product.getPrice() <= maxPrice;
            
            // Apply search filter
            boolean searchMatch = true;
            if (!currentQuery.isEmpty()) {
                searchMatch = false;
                String lowercaseQuery = currentQuery.toLowerCase();
                
                boolean titleMatch = product.getTitle() != null && 
                        product.getTitle().toLowerCase().contains(lowercaseQuery);
                boolean descriptionMatch = product.getDescription() != null && 
                        product.getDescription().toLowerCase().contains(lowercaseQuery);
                boolean categoryTextMatch = product.getCategory() != null && 
                        product.getCategory().toLowerCase().contains(lowercaseQuery);
                
                searchMatch = titleMatch || descriptionMatch || categoryTextMatch;
            }
            
            if (categoryMatch && priceMatch && searchMatch) {
                filteredList.add(product);
            }
        }
        
        // Apply sorting
        sortFilteredList();
        
        if (filteredList.isEmpty()) {
            showEmptyView("No products match your filters");
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private void sortFilteredList() {
        if (sortBy.equals("price")) {
            Collections.sort(filteredList, (p1, p2) -> {
                int result = Double.compare(p1.getPrice(), p2.getPrice());
                return sortAscending ? result : -result;
            });
        } else if (sortBy.equals("timestamp")) {
            Collections.sort(filteredList, (p1, p2) -> {
                int result = Long.compare(p1.getTimestamp(), p2.getTimestamp());
                return sortAscending ? result : -result;
            });
        } else if (sortBy.equals("averageRating")) {
            Collections.sort(filteredList, (p1, p2) -> {
                int result = Float.compare(p1.getAverageRating(), p2.getAverageRating());
                return sortAscending ? result : -result;
            });
        }
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