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

/**
 * אדפטר המציג את רשימת המשמרות עבור העובד.
 * האדפטר מטפל בלוגיקה הוויזואלית של כל שורה:
 * - צבע רקע שונה לכל סטטוס (ירוק=משובץ, כתום=ממתין, לבן=פנוי).
 * - כפתורים המשתנים בהתאם (הרשמה, ביטול, הוספה ליומן).
 */
public class EmployeeShiftsAdapter extends RecyclerView.Adapter<EmployeeShiftsAdapter.ViewHolder> {

    // רשימת המשמרות להצגה
    private List<Shift> shiftsList;

    // המזהה של העובד הנוכחי (כדי לבדוק אם *אני* רשום למשמרת הספציפית)
    private String currentUserId;

    // מאזין ללחיצות (מעביר את הפעולה חזרה ל-Activity)
    private OnShiftActionListener listener;

    /**
     * ממשק (Interface) להגדרת הפעולות האפשריות.
     * ה-Activity שמפעיל את האדפטר חייב לממש את הפונקציות האלו.
     */
    public interface OnShiftActionListener {
        void onSignUp(Shift shift); // בקשת הרשמה
        void onCancel(Shift shift); // ביטול בקשה
    }

    /**
     * בנאי (Constructor)
     * @param shiftsList - הנתונים להצגה.
     * @param currentUserId - ה-ID של העובד המחובר.
     * @param listener - מי מטפל בלחיצות הכפתורים.
     */
    public EmployeeShiftsAdapter(List<Shift> shiftsList, String currentUserId, OnShiftActionListener listener) {
        this.shiftsList = shiftsList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    /**
     * יצירת המראה הוויזואלי של שורה בודדת (ViewHolder).
     * טוען את קובץ ה-XML שנקרא item_employee_shift.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_shift, parent, false);
        return new ViewHolder(view);
    }

    /**
     * חיבור הנתונים לשורה ספציפית (Binding).
     * כאן מתרחשת כל הלוגיקה של "איך השורה נראית".
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shift shift = shiftsList.get(position);

        // 1. הצגת שעות המשמרת (פורמט HH:mm)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(shift.getStartTime()) + " - " + sdf.format(shift.getEndTime()));

        // 2. חישוב תפוסה (כמה רשומים מתוך כמה צריך)
        int currentRegistered = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
        int maxRequired = shift.getRequiredWorkers();

        holder.tvStatus.setText("תפוסה: " + currentRegistered + "/" + maxRequired);

        // 3. בדיקת סטטוס אישי מול המשמרת
        // האם אני ברשימת המאושרים?
        boolean amISignedUp = shift.getAssignedUserIds() != null && shift.getAssignedUserIds().contains(currentUserId);
        // האם אני ברשימת הממתינים?
        boolean amIPending = shift.getPendingUserIds() != null && shift.getPendingUserIds().contains(currentUserId);
        // האם המשמרת מלאה לגמרי?
        boolean isFull = currentRegistered >= maxRequired;

        // איפוס כפתור היומן (מוסתר כברירת מחדל, יוצג רק אם אני משובץ)
        holder.btnCalendar.setVisibility(View.GONE);

        // --- לוגיקת צבעים וכפתורים ---

        if (amISignedUp) {
            // מצב 1: אני משובץ ומאושר למשמרת
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // רקע ירוק בהיר
            holder.btnAction.setText("משובץ ✅");
            holder.btnAction.setBackgroundColor(Color.GRAY); // כפתור אפור (לא לחיץ)
            holder.btnAction.setEnabled(false); // כרגע אין ביטול עצמי דרך הכפתור הזה

            // הצגת כפתור "הוסף ליומן" (כי השיבוץ סופי)
            holder.btnCalendar.setVisibility(View.VISIBLE);
            holder.btnCalendar.setOnClickListener(v -> addToGoogleCalendar(holder.itemView.getContext(), shift));

        } else if (amIPending) {
            // מצב 2: ביקשתי להירשם אבל המנהל טרם אישר
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0")); // רקע כתום בהיר
            holder.btnAction.setText("ממתין... (בטל)");
            holder.btnAction.setBackgroundColor(Color.parseColor("#FF9800")); // כפתור כתום
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onCancel(shift)); // לחיצה מבטלת את הבקשה

        } else if (isFull) {
            // מצב 3: המשמרת מלאה ואני לא רשום אליה
            holder.cardView.setCardBackgroundColor(Color.parseColor("#EEEEEE")); // רקע אפור
            holder.btnAction.setText("מלא");
            holder.btnAction.setBackgroundColor(Color.GRAY);
            holder.btnAction.setEnabled(false); // אי אפשר להירשם

        } else {
            // מצב 4: המשמרת פנויה ואני יכול להירשם
            holder.cardView.setCardBackgroundColor(Color.WHITE); // רקע לבן רגיל
            holder.btnAction.setText("הירשם");
            holder.btnAction.setBackgroundColor(Color.parseColor("#6200EE")); // כפתור סגול
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onSignUp(shift)); // לחיצה שולחת בקשה
        }
    }

    /**
     * פונקציה לפתיחת יומן חיצוני (Google Calendar וכו') והוספת אירוע.
     * משתמשת ב-Intent מובנה של אנדרואיד (CalendarContract).
     */
    private void addToGoogleCalendar(android.content.Context context, Shift shift) {
        try {
            // יצירת כוונה (Intent) להוספת אירוע
            Intent intent = new Intent(Intent.ACTION_INSERT);

            // הגדרת סוג הנתונים כאירועי יומן
            intent.setData(CalendarContract.Events.CONTENT_URI);

            // מילוי פרטי האירוע
            intent.putExtra(CalendarContract.Events.TITLE, "משמרת עבודה - ShiftSync");
            intent.putExtra(CalendarContract.Events.DESCRIPTION, "משמרת שובצה דרך האפליקציה");
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, shift.getStartTime());
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, shift.getEndTime());

            // סימון הזמן כ"לא פנוי" (Busy)
            intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

            // הפעלת האפליקציה החיצונית
            context.startActivity(intent);
        } catch (Exception e) {
            // למקרה שאין שום אפליקציית יומן מותקנת במכשיר
            Toast.makeText(context, "לא נמצאה אפליקציית יומן", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        if (shiftsList == null) return 0;
        return shiftsList.size();
    }

    /**
     * מחלקת ViewHolder - שומרת את ההפניות לרכיבים הגרפיים בשורה.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvStatus;
        Button btnAction;      // כפתור הפעולה הראשי (הירשם/בטל)
        ImageButton btnCalendar; // כפתור הוספה ליומן (האייקון הקטן)
        CardView cardView;     // הכרטיס כולו (לשינוי צבע הרקע)

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnCalendar = itemView.findViewById(R.id.btnAddToCalendar);
            cardView = itemView.findViewById(R.id.cardShift);
        }
    }
}