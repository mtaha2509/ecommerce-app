package com.example.ecommerceapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.ecommerceapp.fragments.OrderListFragment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OrderPagerAdapter extends FragmentStateAdapter {
    private static final int NUM_PAGES = 5;
    
    private Date startDate;
    private Date endDate;
    private Map<Integer, OrderListFragment> fragmentMap = new HashMap<>();
    
    public OrderPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String status;
        switch (position) {
            case 0:
                status = "PENDING";
                break;
            case 1:
                status = "PROCESSING";
                break;
            case 2:
                status = "SHIPPED";
                break;
            case 3:
                status = "COMPLETED";
                break;
            case 4:
            default:
                status = "ALL";
                break;
        }
        
        OrderListFragment fragment = OrderListFragment.newInstance(status, startDate, endDate);
        fragmentMap.put(position, fragment);
        return fragment;
    }
    
    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
    
    /**
     * Set date range filter for orders
     * @param startDate Start date filter (null for no start date filter)
     * @param endDate End date filter (null for no end date filter)
     */
    public void setDateRange(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        
        // Refresh existing fragments with new date filter
        for (OrderListFragment fragment : fragmentMap.values()) {
            if (fragment != null) {
                fragment.updateDateFilter(startDate, endDate);
            }
        }
        
        notifyDataSetChanged();
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < NUM_PAGES;
    }
}