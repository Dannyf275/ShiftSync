package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.ShiftRequestItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * אדפטר (Adapter) לניהול רשימת הבקשות לאישור.
 * תפקידו לקשר בין רשימת הבקשות (List<ShiftRequestItem>) לבין התצוגה הגרפית (RecyclerView).
 * כל שורה ברשימה מציגה:
 * 1. פרטי המשמרת (תאריך ושעה).
 * 2. שם העובד שמבקש להצטרף.
 * 3. כפתורי פעולה (אישור / דחייה).
 */
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    // רשימת הבקשות שמוצגת כרגע
    private List<ShiftRequestItem> requests;

    // המאזין לאירועים (ה-Activity שמממש את הממשק)
    private OnRequestActionListener listener;

    /**
     * ממשק (Interface) להעברת אירועי לחיצה חזרה ל-Activity.
     * האדפטר רק מציג את הכפתורים, אך הלוגיקה של "מה קורה כשמאשרים" (עדכון ב-Firebase)
     * צריכה להתבצע בתוך ה-Activity (ShiftRequestsActivity).
     */
    public interface OnRequestActionListener {
        void onApprove(ShiftRequestItem item); // המנהל לחץ "אשר"
        void onDeny(ShiftRequestItem item);    // המנהל לחץ "דחה"
    }

    /**
     * בנאי (Constructor).
     * @param requests - רשימת הנתונים להצגה.
     * @param listener - מי שמטפל בלחיצות (ה-Activity).
     */
    public RequestsAdapter(List<ShiftRequestItem> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    /**
     * יצירת המראה הוויזואלי של שורה בודדת (ViewHolder).
     * הפונקציה "מנפחת" (Inflates) את קובץ ה-XML שנקרא item_shift_request.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יצירת View מתוך ה-XML
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_request, parent, false);
        return new ViewHolder(view);
    }

    /**
     * חיבור הנתונים לשורה ספציפית (Binding).
     * כאן אנו לוקחים את המידע מהרשימה ומציבים אותו בתוך הטקסטים והכפתורים.
     * @param holder - המחזיק של הרכיבים הגרפיים.
     * @param position - המיקום של השורה ברשימה.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 1. שליפת הפריט הנוכחי מהרשימה
        ShiftRequestItem item = requests.get(position);

        // 2. פרמוט התאריך והשעה להצגה נוחה
        // תבנית לתאריך (למשל: 01/01)
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM", Locale.getDefault());
        // תבנית לשעה (למשל: 08:00)
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // בניית המחרוזת הסופית: "01/01 | 08:00 - 16:00"
        String timeStr = sdfDate.format(item.getShift().getStartTime()) + " | " +
                sdfTime.format(item.getShift().getStartTime()) + " - " +
                sdfTime.format(item.getShift().getEndTime());

        // 3. הצגת הטקסטים במסך
        holder.tvShiftDetails.setText(timeStr);       // פרטי משמרת
        holder.tvEmployeeName.setText(item.getUserName()); // שם העובד המבקש

        // 4. הגדרת הכפתורים
        // בעת לחיצה על "אשר", נקרא לפונקציה onApprove של ה-Activity
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(item));

        // בעת לחיצה על "דחה", נקרא לפונקציה onDeny של ה-Activity
        holder.btnDeny.setOnClickListener(v -> listener.onDeny(item));
    }

    /**
     * מחזירה את מספר הפריטים ברשימה (כדי שה-RecyclerView ידע כמה שורות לצייר).
     */
    @Override
    public int getItemCount() {
        return requests.size();
    }

    /**
     * מחלקת ViewHolder - שומרת את ההפניות לרכיבים הגרפיים בשורה.
     * מונעת את הצורך לחפש את הרכיבים (findViewById) בכל גלילה מחדש.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShiftDetails, tvEmployeeName;
        Button btnApprove, btnDeny;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור בין המשתנים בקוד למזהים (IDs) בקובץ ה-XML
            tvShiftDetails = itemView.findViewById(R.id.tvShiftDetails);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDeny = itemView.findViewById(R.id.btnDeny);
        }
    }
}