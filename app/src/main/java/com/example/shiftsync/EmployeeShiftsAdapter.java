package com.example.shiftsync;

import android.content.Intent;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.Shift;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EmployeeShiftsAdapter extends RecyclerView.Adapter<EmployeeShiftsAdapter.ViewHolder> {

    private List<Shift> shiftsList;
    private String currentUserId;
    private OnShiftActionListener listener;

    public interface OnShiftActionListener {
        void onSignUp(Shift shift);
        void onCancel(Shift shift);
    }

    public EmployeeShiftsAdapter(List<Shift> shiftsList, String currentUserId, OnShiftActionListener listener) {
        this.shiftsList = shiftsList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_shift, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shift shift = shiftsList.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(shift.getStartTime()) + " - " + sdf.format(shift.getEndTime()));

        int currentRegistered = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
        int maxRequired = shift.getRequiredWorkers();

        holder.tvStatus.setText("תפוסה: " + currentRegistered + "/" + maxRequired);

        // בדיקת סטטוסים
        boolean amISignedUp = shift.getAssignedUserIds() != null && shift.getAssignedUserIds().contains(currentUserId);
        boolean amIPending = shift.getPendingUserIds() != null && shift.getPendingUserIds().contains(currentUserId);
        boolean isFull = currentRegistered >= maxRequired;

        // איפוס כפתור יומן (מוסתר כברירת מחדל)
        holder.btnCalendar.setVisibility(View.GONE);

        if (amISignedUp) {
            // --- אני משובץ סופית ---
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.btnAction.setText("משובץ ✅");
            holder.btnAction.setBackgroundColor(Color.GRAY);
            holder.btnAction.setEnabled(false); // אין ביטול עצמי (אפשר לשנות אם רוצים)

            // הצגת כפתור הוספה ליומן
            holder.btnCalendar.setVisibility(View.VISIBLE);
            holder.btnCalendar.setOnClickListener(v -> addToGoogleCalendar(holder.itemView.getContext(), shift));

        } else if (amIPending) {
            // --- ממתין לאישור ---
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            holder.btnAction.setText("ממתין... (בטל)");
            holder.btnAction.setBackgroundColor(Color.parseColor("#FF9800"));
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onCancel(shift));

        } else if (isFull) {
            // --- מלא ---
            holder.cardView.setCardBackgroundColor(Color.parseColor("#EEEEEE"));
            holder.btnAction.setText("מלא");
            holder.btnAction.setBackgroundColor(Color.GRAY);
            holder.btnAction.setEnabled(false);

        } else {
            // --- פנוי ---
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.btnAction.setText("הירשם");
            holder.btnAction.setBackgroundColor(Color.parseColor("#6200EE"));
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onSignUp(shift));
        }
    }

    /**
     * פונקציה שפותחת את אפליקציית היומן עם פרטי המשמרת
     */
    private void addToGoogleCalendar(android.content.Context context, Shift shift) {
        try {
            // התיקון: שימוש ב-ACTION_INSERT (אותיות גדולות)
            Intent intent = new Intent(Intent.ACTION_INSERT);

            intent.setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.Events.TITLE, "משמרת עבודה - ShiftSync");
            intent.putExtra(CalendarContract.Events.DESCRIPTION, "משמרת שובצה דרך האפליקציה");
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, shift.getStartTime());
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, shift.getEndTime());
            intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "לא נמצאה אפליקציית יומן", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        if (shiftsList == null) return 0;
        return shiftsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus;
        Button btnAction;
        ImageButton btnCalendar; // <-- הכפתור החדש
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnCalendar = itemView.findViewById(R.id.btnAddToCalendar); // <-- קישור
            cardView = itemView.findViewById(R.id.cardShift);
        }
    }
}