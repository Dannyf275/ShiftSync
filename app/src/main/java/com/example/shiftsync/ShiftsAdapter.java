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

/**
 * אדפטר לניהול רשימת המשמרות במסך המנהל.
 * האדפטר הזה חכם: הוא יודע לצבוע את המשמרת בצבעים שונים בהתאם למצב השיבוץ שלה
 * (מלאה, חלקית או ריקה).
 */
public class ShiftsAdapter extends RecyclerView.Adapter<ShiftsAdapter.ShiftViewHolder> {

    // --- ממשק (Interface) ---
    // מאפשר ל-Activity להגיב ללחיצה על כפתור המחיקה בתוך השורה
    public interface OnShiftClickListener {
        void onDeleteClick(int position);
    }

    // --- משתנים ---
    private List<Shift> shiftsList;
    private OnShiftClickListener listener;

    // --- בנאי (Constructor) ---
    public ShiftsAdapter(List<Shift> shiftsList, OnShiftClickListener listener) {
        this.shiftsList = shiftsList;
        this.listener = listener;
    }

    // --- מתודות של RecyclerView ---

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינת העיצוב (XML) של שורה בודדת (item_shift)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift, parent, false);
        return new ShiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        Shift shift = shiftsList.get(position);

        // 1. פרמוט והצגת השעות (למשל: 08:00 - 16:00)
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String start = timeFormat.format(shift.getStartTime());
        String end = timeFormat.format(shift.getEndTime());
        holder.tvTime.setText(start + " - " + end);

        // 2. חישוב סטטוס תפוסה (Occupancy)
        // בודקים כמה עובדים כבר רשומים (assigned) מתוך כמה שנדרש (required)
        // הערה: השימוש ב-? מונע קריסה אם הרשימה null
        int current = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
        int required = shift.getRequiredWorkers();

        // עדכון הטקסט (למשל: "שיבוץ: 1/3")
        holder.tvStatus.setText("שיבוץ: " + current + "/" + required);

        // 3. לוגיקת צבעים לפי מצב המשמרת
        if (current >= required) {
            // מצב: מלא (Full) -> צבע ירוק
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // רקע ירוק בהיר
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));       // טקסט ירוק כהה
            // (אופציונלי: אפשר להוסיף אייקון V לטקסט)
        } else if (current > 0) {
            // מצב: חלקי (Partial) -> צבע כתום
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0")); // רקע כתום בהיר
            holder.tvStatus.setTextColor(Color.parseColor("#EF6C00"));       // טקסט כתום כהה
        } else {
            // מצב: ריק (Empty) -> צבע לבן רגיל
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvStatus.setTextColor(Color.DKGRAY);
        }

        // 4. הגדרת כפתור המחיקה
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        // בדיקת בטיחות למקרה שהרשימה טרם אותחלה
        if (shiftsList == null) return 0;
        return shiftsList.size();
    }

    // --- ViewHolder ---
    public static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus;
        CardView cardView;
        ImageButton btnDelete;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור לרכיבים ב-XML
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
            cardView = itemView.findViewById(R.id.cardShift);
            btnDelete = itemView.findViewById(R.id.btnDeleteShift);
        }
    }
}