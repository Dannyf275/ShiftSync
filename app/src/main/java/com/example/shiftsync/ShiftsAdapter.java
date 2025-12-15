package com.example.shiftsync;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.Shift;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ShiftsAdapter extends RecyclerView.Adapter<ShiftsAdapter.ShiftViewHolder> {

    // עדכון הממשק: הוספנו את onClick (לחיצה על הכרטיס עצמו)
    public interface OnShiftClickListener {
        void onDeleteClick(int position); // מחיקת כל המשמרת
        void onShiftClick(Shift shift);   // צפייה בפרטי המשמרת
    }

    private List<Shift> shiftsList;
    private OnShiftClickListener listener;

    public ShiftsAdapter(List<Shift> shiftsList, OnShiftClickListener listener) {
        this.shiftsList = shiftsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift, parent, false);
        return new ShiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        Shift shift = shiftsList.get(position);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String start = timeFormat.format(shift.getStartTime());
        String end = timeFormat.format(shift.getEndTime());
        holder.tvTime.setText(start + " - " + end);

        int current = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
        int required = shift.getRequiredWorkers();

        holder.tvStatus.setText("שיבוץ: " + current + "/" + required);

        if (current >= required) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else if (current > 0) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            holder.tvStatus.setTextColor(Color.parseColor("#EF6C00"));
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvStatus.setTextColor(Color.DKGRAY);
        }

        // כפתור מחיקת משמרת
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(position));

        // --- הוספה חדשה: לחיצה על הכרטיס כולו ---
        holder.itemView.setOnClickListener(v -> listener.onShiftClick(shift));
    }

    @Override
    public int getItemCount() {
        if (shiftsList == null) return 0;
        return shiftsList.size();
    }

    public static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus;
        CardView cardView;
        ImageButton btnDelete;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
            cardView = itemView.findViewById(R.id.cardShift);
            btnDelete = itemView.findViewById(R.id.btnDeleteShift);
        }
    }
}