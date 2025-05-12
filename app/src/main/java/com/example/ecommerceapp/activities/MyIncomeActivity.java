package com.example.ecommerceapp.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.IncomePagerAdapter;
import com.example.ecommerceapp.fragments.ReleasedIncomeFragment;
import com.example.ecommerceapp.fragments.ToReleaseIncomeFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MyIncomeActivity extends AppCompatActivity {
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_income);
        
        // Set title
        setTitle("My Income");
        
        // Initialize views
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        
        // Setup ViewPager with fragments
        IncomePagerAdapter adapter = new IncomePagerAdapter(this);
        adapter.addFragment(new ToReleaseIncomeFragment(), "To Release");
        adapter.addFragment(new ReleasedIncomeFragment(), "Released");
        viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(adapter.getPageTitle(position));
        }).attach();
    }
} 