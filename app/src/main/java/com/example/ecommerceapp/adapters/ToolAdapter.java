package com.example.ecommerceapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Tool;

import java.util.List;

public class ToolAdapter extends RecyclerView.Adapter<ToolAdapter.ToolViewHolder> {
    private List<Tool> tools;
    private OnToolClickListener listener;

    public interface OnToolClickListener {
        void onToolClick(Tool tool);
    }

    public ToolAdapter(List<Tool> tools, OnToolClickListener listener) {
        this.tools = tools;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tool, parent, false);
        return new ToolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ToolViewHolder holder, int position) {
        Tool tool = tools.get(position);
        holder.icon.setImageResource(tool.getIconResId());
        holder.name.setText(tool.getName());
        holder.itemView.setOnClickListener(v -> listener.onToolClick(tool));
    }

    @Override
    public int getItemCount() {
        return tools.size();
    }

    static class ToolViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        ToolViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_tool_icon);
            name = itemView.findViewById(R.id.tv_tool_name);
        }
    }
} 