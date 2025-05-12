package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.OrderPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class OrderManagementActivity extends AppCompatActivity {
    private static final String TAG = "OrderManagementActivity";
    
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabFilter;
    
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    private OrderPagerAdapter pagerAdapter;
    
    // Filter variables
    private Date startDate = null;
    private Date endDate = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_management);
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login to manage orders", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        fabFilter = findViewById(R.id.fabFilter);
        
        // Set up toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Set up ViewPager with tabs
        setupViewPager();
        
        // Set up filter FAB
        fabFilter.setOnClickListener(v -> showFilterDialog());
    }
    
    private void setupViewPager() {
        // Create adapter
        pagerAdapter = new OrderPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Pending");
                    break;
                case 1:
                    tab.setText("Processing");
                    break;
                case 2:
                    tab.setText("Shipped");
                    break;
                case 3:
                    tab.setText("Completed");
                    break;
                case 4:
                    tab.setText("All");
                    break;
            }
        }).attach();
    }
    
    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Orders");
        
        // Inflate custom layout for the dialog
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_order_filter, null);
        LinearLayout dateRangeLayout = view.findViewById(R.id.dateRangeLayout);
        TextView tvStartDate = view.findViewById(R.id.tvStartDate);
        TextView tvEndDate = view.findViewById(R.id.tvEndDate);
        
        // Set current filter values if any
        if (startDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            tvStartDate.setText(String.format("%d/%d/%d", 
                    cal.get(Calendar.MONTH) + 1, 
                    cal.get(Calendar.DAY_OF_MONTH), 
                    cal.get(Calendar.YEAR)));
        }
        
        if (endDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            tvEndDate.setText(String.format("%d/%d/%d", 
                    cal.get(Calendar.MONTH) + 1, 
                    cal.get(Calendar.DAY_OF_MONTH), 
                    cal.get(Calendar.YEAR)));
        }
        
        // Set click listeners for date selection
        tvStartDate.setOnClickListener(v -> showDatePicker(true, tvStartDate));
        tvEndDate.setOnClickListener(v -> showDatePicker(false, tvEndDate));
        
        builder.setView(view);
        
        // Add buttons
        builder.setPositiveButton("Apply", (dialog, which) -> {
            // Apply filters
            if (pagerAdapter != null) {
                pagerAdapter.setDateRange(startDate, endDate);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        
        builder.setNeutralButton("Clear Filters", (dialog, which) -> {
            // Clear filters
            startDate = null;
            endDate = null;
            if (pagerAdapter != null) {
                pagerAdapter.setDateRange(null, null);
            }
        });
        
        builder.show();
    }
    
    private void showDatePicker(boolean isStartDate, TextView dateTextView) {
        final Calendar calendar = Calendar.getInstance();
        
        // Set initial date if already selected
        if (isStartDate && startDate != null) {
            calendar.setTime(startDate);
        } else if (!isStartDate && endDate != null) {
            calendar.setTime(endDate);
        }
        
        DatePicker datePicker = new DatePicker(this);
        datePicker.init(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                null
        );
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(datePicker);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            
            if (isStartDate) {
                // Set time to beginning of day
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startDate = calendar.getTime();
            } else {
                // Set time to end of day
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                endDate = calendar.getTime();
            }
            
            // Update date display
            dateTextView.setText(String.format("%d/%d/%d", 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH), 
                    calendar.get(Calendar.YEAR)));
        });
        
        builder.setNegativeButton("Cancel", null);
        
        builder.show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 