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
import com.google.android.material.button.MaterialButton;

public class ToolsFragment extends Fragment {

    private MaterialButton btnAddProduct;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);
        
        btnAddProduct = view.findViewById(R.id.btn_add_product);
        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddProductActivity.class);
            startActivity(intent);
        });
        
        return view;
    }
}
