package com.example.ecommerceapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.ecommerceapp.R;

public class ToolsFragment extends Fragment {

    private GridLayout basicFunctionGrid;
    private String[] basicTools = {"Add Products", "Products", "Orders"};
    private int[] basicIcons = {
            R.drawable.cart_plus,
            R.drawable.storefront_24px,
            R.drawable.orders_24px,
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);
        basicFunctionGrid = view.findViewById(R.id.basic_function_grid);
        return view;
    }
    private void addToolItems(LayoutInflater inflater, GridLayout grid, String[] names, int[] icons) {
        for (int i = 0; i < names.length; i++) {

            View item = inflater.inflate(R.layout.item_tool, null);
            ImageView icon = item.findViewById(R.id.img_tool_icon);
            TextView label = item.findViewById(R.id.tv_tool_name);

            icon.setImageResource(icons[i]);
            label.setText(names[i]);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            item.setLayoutParams(params);

            // Add to grid
            grid.addView(item);

            // Optional: Add click listener
            int index = i; // for lambda capture
            item.setOnClickListener(v -> {
                // Handle click event here (e.g., open AddProductsActivity)
                // Example:
                // startActivity(new Intent(getActivity(), AddProductsActivity.class));
            });
        }
    }
}
