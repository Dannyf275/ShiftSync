package com.example.shiftsync;

import android.graphics.Color;
import android.text.TextUtils;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * אדפטר (Adapter) לניהול רשימת המשמרות ב-RecyclerView.
 * משמש גם את המנהל (ManagerScheduleActivity) וגם את העובד (EmployeeScheduleActivity).
 * האדפטר אחראי על העיצוב הוויזואלי של כל "קוביה" בלוח המשמרות.
 */
public class ShiftsAdapter extends RecyclerView.Adapter<ShiftsAdapter.ShiftViewHolder> {

    /**
     * ממשק (Interface) להגדרת הפעולות האפשריות על משמרת.
     * ה-Activity שמפעיל את האדפטר חייב לממש את הפונקציות האלו.
     */
    public interface OnShiftClickListener {
        void onDeleteClick(int position); // מחיקת משמרת
        void onEditClick(Shift shift);    // עריכת משמרת (שעות/כמות עובדים/הערות)
        void onShiftClick(Shift shift);   // לחיצה כללית על המשמרת (לפתיחת פרטים או הרשמה)
    }

    // רשימת המשמרות להצגה
    private List<Shift> shiftsList;

    // המאזין לאירועים (ה-Activity)
    private OnShiftClickListener listener;

    /**
     * בנאי (Constructor).
     * @param shiftsList - רשימת הנתונים.
     * @param listener - מי שמטפל בלחיצות.
     */
    public ShiftsAdapter(List<Shift> shiftsList, OnShiftClickListener listener) {
        this.shiftsList = shiftsList;
        this.listener = listener;
    }

    /**
     * יצירת המראה הוויזואלי של שורה בודדת (ViewHolder).
     * טוען את קובץ ה-XML שנקרא item_shift.
     */
    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift, parent, false);
        return new ShiftViewHolder(view);
    }

    /**
     * חיבור הנתונים לשורה ספציפית (Binding).
     * כאן מתרחשת כל הלוגיקה של העיצוב (צבעים, טקסטים).
     */
    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        // 1. שליפת המשמרת הנוכחית
        Shift shift = shiftsList.get(position);

        // 2. הצגת השעות בפורמט HH:mm (למשל 08:00 - 16:00)
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String start = timeFormat.format(shift.getStartTime());
        String end = timeFormat.format(shift.getEndTime());
        holder.tvTime.setText(start + " - " + end);

        // 3. לוגיקה חכמה לצבע רקע לפי שעת היום
        // אנו בודקים מתי המשמרת מתחילה כדי לתת צבע מתאים.
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(shift.getStartTime());
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String colorHex;
        if (hour >= 6 && hour < 12) {
            colorHex = "#FFF8E1"; // בוקר (צהבהב)
        } else if (hour >= 12 && hour < 17) {
            colorHex = "#E3F2FD"; // צהריים (כחלחל)
        } else if (hour >= 17 && hour < 21) {
            colorHex = "#F3E5F5"; // ערב (סגלגל)
        } else {
            colorHex = "#ECEFF1"; // לילה (אפרפר)
        }

        // קביעת צבע הרקע של הכרטיס
        holder.cardView.setCardBackgroundColor(Color.parseColor(colorHex));

        // 4. חישוב סטטוס תפוסה (כמה רשומים מתוך כמה שצריך)
        int current = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
        int required = shift.getRequiredWorkers();

        if (current >= required) {
            // אם המשמרת מלאה -> טקסט ירוק
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvStatus.setText("שיבוץ: מלא ✅");
        } else {
            // אם חסרים עובדים -> טקסט כתום עם פירוט המספרים
            holder.tvStatus.setTextColor(Color.parseColor("#E65100"));
            holder.tvStatus.setText("שיבוץ: " + current + "/" + required);
        }

        // 5. הצגת הערות מנהל (אם קיימות)
        // משתמשים ב-TextUtils.isEmpty כדי לבדוק גם null וגם מחרוזת ריקה בבת אחת.
        if (!TextUtils.isEmpty(shift.getNotes())) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("📝 הערה: " + shift.getNotes());
        } else {
            // אם אין הערה, מסתירים את השדה כדי שלא יתפוס מקום סתם.
            holder.tvNotes.setVisibility(View.GONE);
        }

        // 6. הגדרת המאזינים לכפתורים
        // לחיצה על פח אשפה (מחיקה)
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(position));

        // לחיצה על עיפרון (עריכה)
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(shift));

        // לחיצה על הכרטיס כולו (לפתיחת רשימת עובדים או הרשמה)
        holder.itemView.setOnClickListener(v -> listener.onShiftClick(shift));
    }

    /**
     * כמות הפריטים ברשימה.
     */
    @Override
    public int getItemCount() {
        return shiftsList == null ? 0 : shiftsList.size();
    }

    /**
     * מחלקת ViewHolder - שומרת את ההפניות לרכיבים הגרפיים בשורה.
     */
    public static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus, tvNotes;
        CardView cardView;
        ImageButton btnDelete, btnEdit;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור לרכיבים בקובץ item_shift.xml
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
            tvNotes = itemView.findViewById(R.id.tvShiftNotes); // שדה ההערות
            cardView = itemView.findViewById(R.id.cardShift);

            // כפתורי ניהול (מוצגים/מוסתרים ב-XML או בניהול לוגי אחר אם צריך)
            btnDelete = itemView.findViewById(R.id.btnDeleteShift);
            btnEdit = itemView.findViewById(R.id.btnEditShift);
        }
    }
}