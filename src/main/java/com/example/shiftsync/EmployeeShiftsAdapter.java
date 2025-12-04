package com.example.shiftsync;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.Shift;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * אדפטר לניהול רשימת המשמרות במסך העובד.
 * המחלקה הזו אחראית על הלוגיקה הוויזואלית:
 * 1. האם העובד כבר רשום? -> הראה כפתור ביטול.
 * 2. האם המשמרת מלאה? -> הראה כפתור אפור (לא ניתן להירשם).
 * 3. האם המשמרת פנויה? -> הראה כפתור הרשמה.
 */
public class EmployeeShiftsAdapter extends RecyclerView.Adapter<EmployeeShiftsAdapter.ViewHolder> {

    // --- משתנים (Fields) ---
    private List<Shift> shiftsList;    // רשימת המשמרות להצגה
    private String currentUserId;      // המזהה של העובד הנוכחי (כדי לדעת אם הוא כבר רשום)
    private OnShiftActionListener listener; // הממשק שדרכו נעביר לחיצות ל-Activity

    // --- ממשק (Interface) ---
    // מגדיר אילו פעולות העובד יכול לבצע על משמרת
    public interface OnShiftActionListener {
        void onSignUp(Shift shift);  // פעולת הרשמה
        void onCancel(Shift shift);  // פעולת ביטול הרשמה
    }

    // --- בנאי (Constructor) ---
    public EmployeeShiftsAdapter(List<Shift> shiftsList, String currentUserId, OnShiftActionListener listener) {
        this.shiftsList = shiftsList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    // --- יצירת התצוגה (Create View) ---
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינת קובץ ה-XML שיצרנו עבור שורת משמרת לעובד
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_shift, parent, false);
        return new ViewHolder(view);
    }

    // --- חיבור הנתונים (Bind Data) ---
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shift shift = shiftsList.get(position);

        // 1. הצגת השעות בפורמט HH:mm
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String start = sdf.format(shift.getStartTime());
        String end = sdf.format(shift.getEndTime());
        holder.tvTime.setText(start + " - " + end);

        // 2. חישוב נתוני תפוסה
        // כמה עובדים רשומים כרגע? (בדיקת null למניעת קריסה)
        int currentRegistered = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
        int maxRequired = shift.getRequiredWorkers();

        holder.tvStatus.setText("תפוסה: " + currentRegistered + "/" + maxRequired);

        // 3. לוגיקה לקביעת מצב הכפתור והצבעים

        // בדיקה: האם ה-ID שלי נמצא ברשימת העובדים של המשמרת?
        boolean amISignedUp = shift.getAssignedUserIds() != null && shift.getAssignedUserIds().contains(currentUserId);

        // בדיקה: האם המשמרת מלאה (ורק אם אני לא רשום אליה)?
        boolean isFull = currentRegistered >= maxRequired;

        if (amISignedUp) {
            // --- מצב 1: אני כבר רשום למשמרת ---
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // רקע ירוק בהיר
            holder.btnAction.setText("בטל רישום");
            holder.btnAction.setBackgroundColor(Color.parseColor("#D32F2F")); // כפתור אדום
            holder.btnAction.setEnabled(true);

            // הגדרת פעולת הביטול
            holder.btnAction.setOnClickListener(v -> listener.onCancel(shift));

        } else if (isFull) {
            // --- מצב 2: המשמרת מלאה ואני לא בפנים ---
            holder.cardView.setCardBackgroundColor(Color.parseColor("#EEEEEE")); // רקע אפור
            holder.btnAction.setText("מלא");
            holder.btnAction.setBackgroundColor(Color.GRAY); // כפתור אפור
            holder.btnAction.setEnabled(false); // כפתור מנוטרל (אי אפשר ללחוץ)

            holder.btnAction.setOnClickListener(null); // הסרת מאזין

        } else {
            // --- מצב 3: המשמרת פנויה ואני יכול להירשם ---
            holder.cardView.setCardBackgroundColor(Color.WHITE); // רקע רגיל
            holder.btnAction.setText("הירשם");
            holder.btnAction.setBackgroundColor(Color.parseColor("#6200EE")); // כפתור סגול
            holder.btnAction.setEnabled(true);

            // הגדרת פעולת ההרשמה
            holder.btnAction.setOnClickListener(v -> listener.onSignUp(shift));
        }
    }

    @Override
    public int getItemCount() {
        if (shiftsList == null) return 0;
        return shiftsList.size();
    }

    // --- ViewHolder ---
    // מחזיק את הרכיבים הגרפיים של השורה
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus;
        Button btnAction;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
            btnAction = itemView.findViewById(R.id.btnAction); // הכפתור המשתנה (הירשם/בטל)
            cardView = itemView.findViewById(R.id.cardShift);
        }
    }
}