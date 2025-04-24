package com.example.ecommerceapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.activities.AddProductActivity;
import com.example.ecommerceapp.adapters.ToolAdapter;
import com.example.ecommerceapp.models.Tool;

import java.util.ArrayList;
import java.util.List;

public class ToolsFragment extends Fragment implements ToolAdapter.OnToolClickListener {

    private RecyclerView toolsRecyclerView;
    private ToolAdapter toolAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);
        
        toolsRecyclerView = view.findViewById(R.id.tools_recycler_view);
        toolsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        List<Tool> tools = new ArrayList<>();
        tools.add(new Tool("Add Products", R.drawable.cart_plus));
        tools.add(new Tool("Products", R.drawable.storefront_24px));
        tools.add(new Tool("Orders", R.drawable.orders_24px));
        
        toolAdapter = new ToolAdapter(tools, this);
        toolsRecyclerView.setAdapter(toolAdapter);
        
        return view;
    }

    @Override
    public void onToolClick(Tool tool) {
        if (tool.getName().equals("Add Products")) {
            Intent intent = new Intent(getActivity(), AddProductActivity.class);
            startActivity(intent);
        } else if (tool.getName().equals("Products")) {
            // TODO: Handle Products click
        } else if (tool.getName().equals("Orders")) {
            // TODO: Handle Orders click
        }
    }
}
