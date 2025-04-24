package com.example.ecommerceapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.ecommerceapp.adapters.ViewPagerAdapter;
import com.example.ecommerceapp.fragments.HomeFragment;
import com.example.ecommerceapp.fragments.ToolsFragment;
import com.example.ecommerceapp.fragments.MessagesFragment;
import com.example.ecommerceapp.fragments.ProfileFragment;
import com.example.ecommerceapp.R;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        viewPager = findViewById(R.id.view_pager);

        // Create fragments
        Fragment[] fragments = {
            new HomeFragment(),
            new ToolsFragment(),
            new MessagesFragment(),
            new ProfileFragment()
        };

        // Setup ViewPager2
        viewPagerAdapter = new ViewPagerAdapter(this, fragments);
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setUserInputEnabled(false); // Disable swipe between fragments

        // Sync ViewPager2 with BottomNavigationView
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0, false);
            } else if (itemId == R.id.nav_tools) {
                viewPager.setCurrentItem(1, false);
            } else if (itemId == R.id.nav_messages) {
                viewPager.setCurrentItem(2, false);
            } else if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(3, false);
            }
            return true;
        });

        // Sync BottomNavigationView with ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_home);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_tools);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_messages);
                        break;
                    case 3:
                        bottomNav.setSelectedItemId(R.id.nav_profile);
                        break;
                }
            }
        });
    }
}
