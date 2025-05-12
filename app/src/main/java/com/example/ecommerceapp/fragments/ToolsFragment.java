package com.example.ecommerceapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.activities.AddProductActivity;
import com.example.ecommerceapp.activities.MyIncomeActivity;
import com.example.ecommerceapp.activities.MyProductsActivity;
import com.google.android.material.button.MaterialButton;

public class ToolsFragment extends Fragment {

    private MaterialButton btnAddProduct, btnViewProducts, btnMyIncome;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);
        
        // Initialize buttons
        btnAddProduct = view.findViewById(R.id.btn_add_product);
        btnViewProducts = view.findViewById(R.id.btn_view_products);
        btnMyIncome = view.findViewById(R.id.btn_my_income);
        
        // Set click listeners
        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddProductActivity.class);
            startActivity(intent);
        });
        
        btnViewProducts.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyProductsActivity.class);
            startActivity(intent);
        });
        
        btnMyIncome.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyIncomeActivity.class);
            startActivity(intent);
        });
        
        return view;
    }
}
