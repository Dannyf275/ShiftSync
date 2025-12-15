package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ShiftEmployeesAdapter extends RecyclerView.Adapter<ShiftEmployeesAdapter.ViewHolder> {

    // מחלקה פנימית קטנה לייצוג עובד ברשימה
    public static class EmployeeItem {
        public String id;
        public String name;
        public EmployeeItem(String id, String name) { this.id = id; this.name = name; }
    }

    private List<EmployeeItem> employees;
    private OnRemoveListener listener;

    public interface OnRemoveListener {
        void onRemove(EmployeeItem employee);
    }

    public ShiftEmployeesAdapter(List<EmployeeItem> employees, OnRemoveListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_employee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeItem item = employees.get(position);
        holder.tvName.setText(item.name);

        holder.btnRemove.setOnClickListener(v -> listener.onRemove(item));
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            btnRemove = itemView.findViewById(R.id.btnRemoveEmployee);
        }
    }
}