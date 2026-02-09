package com.example.shiftsync;

import android.content.Context;
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

// ייבוא הספריות החיוניות (וודא שהן קיימות ב-build.gradle)
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.Shift;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * אדפטר לרשימת המשמרות בתצוגת עובד.
 * מציג סטטוסים שונים (פנוי/ממתין/משובץ) ומאפשר הוספה ליומן גוגל.
 */
public class EmployeeShiftsAdapter extends RecyclerView.Adapter<EmployeeShiftsAdapter.ViewHolder> {

    private List<Shift> shiftsList;
    private String currentUserId;
    private OnShiftActionListener listener;

    // ממשק להעברת אירועי לחיצה ל-Activity
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
        // טעינת קובץ העיצוב (XML) של שורת משמרת לעובד
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_shift, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shift shift = shiftsList.get(position);

        // 1. הצגת זמני המשמרת
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(shift.getStartTime()) + " - " + sdf.format(shift.getEndTime()));

        // 2. חישוב והצגת תפוסה
        int currentRegistered = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
        int maxRequired = shift.getRequiredWorkers();
        holder.tvStatus.setText("תפוסה: " + currentRegistered + "/" + maxRequired);

        // 3. בדיקת הסטטוס של המשתמש הנוכחי מול המשמרת
        boolean amISignedUp = shift.getAssignedUserIds() != null && shift.getAssignedUserIds().contains(currentUserId);
        boolean amIPending = shift.getPendingUserIds() != null && shift.getPendingUserIds().contains(currentUserId);
        boolean isFull = currentRegistered >= maxRequired;

        // --- ברירת מחדל: הסתרת כפתור היומן ---
        // (הכפתור יופיע רק אם המשתמש משובץ סופית)
        holder.btnCalendar.setVisibility(View.GONE);

        // 4. לוגיקה לשינוי המראה לפי הסטטוס
        if (amISignedUp) {
            // === מקרה 1: המשתמש משובץ (מאושר) ===
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // ירוק בהיר
            holder.btnAction.setText("משובץ ✅");
            holder.btnAction.setBackgroundColor(Color.GRAY);
            holder.btnAction.setEnabled(false); // אין צורך בפעולה נוספת על הכפתור הראשי

            // הצגת כפתור היומן!
            holder.btnCalendar.setVisibility(View.VISIBLE);
            // לחיצה עליו תוסיף את האירוע ליומן
            holder.btnCalendar.setOnClickListener(v -> addToGoogleCalendar(holder.itemView.getContext(), shift));

        } else if (amIPending) {
            // === מקרה 2: המשתמש ממתין לאישור ===
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0")); // כתום בהיר
            holder.btnAction.setText("ממתין... (בטל)");
            holder.btnAction.setBackgroundColor(Color.parseColor("#FF9800")); // כתום
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onCancel(shift));

        } else if (isFull) {
            // === מקרה 3: המשמרת מלאה ואין מקום ===
            holder.cardView.setCardBackgroundColor(Color.parseColor("#EEEEEE")); // אפור
            holder.btnAction.setText("מלא");
            holder.btnAction.setBackgroundColor(Color.GRAY);
            holder.btnAction.setEnabled(false);

        } else {
            // === מקרה 4: המשמרת פנויה להרשמה ===
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.btnAction.setText("הירשם");
            holder.btnAction.setBackgroundColor(Color.parseColor("#6200EE")); // סגול
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onSignUp(shift));
        }
    }

    /**
     * פונקציה להוספת המשמרת ליומן גוגל (או כל יומן אחר במכשיר).
     */
    private void addToGoogleCalendar(Context context, Shift shift) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);

            // מילוי פרטי האירוע
            intent.putExtra(CalendarContract.Events.TITLE, "משמרת עבודה - ShiftSync");
            intent.putExtra(CalendarContract.Events.DESCRIPTION, "משמרת שובצה דרך האפליקציה");
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, shift.getStartTime());
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, shift.getEndTime());
            intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

            // פתיחת האפליקציה החיצונית
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "לא נמצאה אפליקציית יומן", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return shiftsList == null ? 0 : shiftsList.size();
    }

    /**
     * מחלקת ה-ViewHolder: מחזיקה את הפניות לרכיבים הגרפיים.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus;
        Button btnAction;
        ImageButton btnCalendar; // כפתור היומן
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
            cardView = itemView.findViewById(R.id.cardShift);

            // חשוב: וודא שה-ID הזה קיים ב-item_employee_shift.xml
            btnCalendar = itemView.findViewById(R.id.btnAddToCalendar);
        }
    }
}