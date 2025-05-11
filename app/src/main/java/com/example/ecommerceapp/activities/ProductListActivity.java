package com.example.ecommerceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.ProductAdapter;
import com.example.ecommerceapp.models.Product;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductListActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ProductAdapter adapter;
    private List<Product> productList;
    private List<Product> filteredList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FloatingActionButton fabLogout, fabFilter, fabCart;
    private Toolbar toolbar;
    private SearchView searchView;
    
    // Filter variables
    private String currentCategory = "";
    private float minPrice = 0;
    private float maxPrice = Float.MAX_VALUE;
    private String sortBy = "timestamp";
    private boolean sortAscending = false;
    private Set<String> categoriesList = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        fabLogout = findViewById(R.id.fab_logout);
        fabFilter = findViewById(R.id.fab_filter);
        fabCart = findViewById(R.id.fab_cart);

        // Setup RecyclerView with 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new ProductAdapter(this, filteredList);
        adapter.setOnProductClickListener(this);
        recyclerView.setAdapter(adapter);

        // Setup button click listeners
        fabLogout.setOnClickListener(v -> logoutUser());
        fabFilter.setOnClickListener(v -> showFilterDialog());
        fabCart.setOnClickListener(v -> {
            Intent intent = new Intent(ProductListActivity.this, CartActivity.class);
            startActivity(intent);
        });

        loadProducts();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_list, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
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
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_sort_price_asc) {
            sortBy = "price";
            sortAscending = true;
            applyFilters();
            return true;
        } else if (id == R.id.action_sort_price_desc) {
            sortBy = "price";
            sortAscending = false;
            applyFilters();
            return true;
        } else if (id == R.id.action_sort_newest) {
            sortBy = "timestamp";
            sortAscending = false;
            applyFilters();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        auth.signOut();
        Intent intent = new Intent(ProductListActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        dialog.setContentView(view);
        
        // Initialize filter views
        ChipGroup categoryChipGroup = view.findViewById(R.id.chipGroupCategories);
        RangeSlider priceRangeSlider = view.findViewById(R.id.priceRangeSlider);
        
        // Dynamically add category chips
        categoryChipGroup.removeAllViews();
        
        // Add "All" category chip
        Chip allChip = new Chip(this);
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked(currentCategory.isEmpty());
        categoryChipGroup.addView(allChip);
        
        // Add other category chips
        for (String category : categoriesList) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(category.equals(currentCategory));
            categoryChipGroup.addView(chip);
        }
        
        // Set price range
        if (maxPrice < Float.MAX_VALUE) {
            priceRangeSlider.setValues(minPrice, maxPrice);
        }
        
        // Set up listener for category selection
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.size() > 0) {
                Chip selectedChip = view.findViewById(checkedIds.get(0));
                currentCategory = "All".equals(selectedChip.getText().toString()) ? "" : selectedChip.getText().toString();
            } else {
                currentCategory = "";
            }
        });
        
        // Apply button
        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            List<Float> values = priceRangeSlider.getValues();
            minPrice = values.get(0);
            maxPrice = values.get(1);
            
            applyFilters();
            dialog.dismiss();
        });
        
        // Reset button
        view.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            currentCategory = "";
            minPrice = 0;
            maxPrice = Float.MAX_VALUE;
            sortBy = "timestamp";
            sortAscending = false;
            
            applyFilters();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    categoriesList.clear();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                        
                        // Collect all categories for filtering
                        if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                            categoriesList.add(product.getCategory());
                        }
                    }
                    
                    // Apply any active filters
                    applyFilters();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading products: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void applyFilters() {
        filteredList.clear();
        
        // Apply category and price filters
        for (Product product : productList) {
            boolean categoryMatch = currentCategory.isEmpty() || 
                    (product.getCategory() != null && product.getCategory().equals(currentCategory));
            boolean priceMatch = product.getPrice() >= minPrice && product.getPrice() <= maxPrice;
            
            if (categoryMatch && priceMatch) {
                filteredList.add(product);
            }
        }
        
        // Apply search filter if search is active
        if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
            filterProducts(searchView.getQuery().toString());
            return;
        }
        
        // Apply sorting
        if (sortBy.equals("price")) {
            if (sortAscending) {
                Collections.sort(filteredList, (p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice()));
            } else {
                Collections.sort(filteredList, (p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));
            }
        } else if (sortBy.equals("timestamp")) {
            if (sortAscending) {
                Collections.sort(filteredList, (p1, p2) -> Long.compare(p1.getTimestamp(), p2.getTimestamp()));
            } else {
                Collections.sort(filteredList, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));
            }
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
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("productId", product.getId());
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(); // Reload products when returning to this activity
    }
}