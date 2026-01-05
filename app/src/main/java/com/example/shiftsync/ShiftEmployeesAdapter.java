package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * אדפטר לרשימת עובדים בתוך משמרת ספציפית.
 * משמש את הדיאלוג שנפתח כאשר מנהל לוחץ על משמרת בלוח השיבוצים.
 * מציג את שם העובד ומאפשר להסיר אותו מהמשמרת.
 */
public class ShiftEmployeesAdapter extends RecyclerView.Adapter<ShiftEmployeesAdapter.ViewHolder> {

    /**
     * מחלקה פנימית פשוטה (Inner Class) לייצוג עובד בתוך הרשימה הזו.
     * אנחנו משתמשים במחלקה זו במקום ב-User המלא, כי ברשימת המשמרות ב-Firebase
     * שמורים רק ה-ID והשם (ולא כל פרטי המשתמש).
     */
    public static class EmployeeItem {
        public String id;   // המזהה הייחודי (UID) - נדרש כדי לבצע מחיקה מהדאטה-בייס
        public String name; // השם לתצוגה

        public EmployeeItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * ממשק (Interface) לטיפול בלחיצה על כפתור המחיקה.
     * האדפטר לא מוחק בעצמו, אלא מודיע ל-Activity: "המשתמש לחץ על הפח בשורה של דני".
     * ה-Activity הוא זה שיבצע את המחיקה בפועל מ-Firestore.
     */
    public interface OnRemoveClickListener {
        void onRemoveClick(EmployeeItem item);
    }

    // רשימת העובדים להצגה
    private List<EmployeeItem> employees;

    // המאזין לאירועים (ה-Activity)
    private OnRemoveClickListener listener;

    /**
     * בנאי (Constructor).
     * @param employees - רשימת העובדים ששולפו מהמשמרת.
     * @param listener - הפונקציה שתופעל כשלוחצים על הסרה.
     */
    public ShiftEmployeesAdapter(List<EmployeeItem> employees, OnRemoveClickListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    /**
     * יצירת המראה הוויזואלי של שורה בודדת.
     * מנפח את הקובץ item_shift_employee.xml.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינת קובץ העיצוב של השורה הבודדת
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_employee, parent, false);
        return new ViewHolder(view);
    }

    /**
     * חיבור הנתונים לשורה ספציפית (Binding).
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // שליפת הפריט הנוכחי
        EmployeeItem item = employees.get(position);

        // הצגת שם העובד ב-TextView
        holder.tvName.setText(item.name);

        // הגדרת פעולה לכפתור המחיקה (פח אשפה)
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                // הפעלת הפונקציה ב-Activity והעברת הפריט למחיקה
                listener.onRemoveClick(item);
            }
        });
    }

    /**
     * מספר הפריטים ברשימה.
     */
    @Override
    public int getItemCount() {
        return employees == null ? 0 : employees.size();
    }

    /**
     * מחלקת ה-ViewHolder שמחזיקה את הרכיבים הגרפיים של השורה.
     * שומרת הפניות ל-TextView ול-ImageButton כדי לחסוך קריאות ל-findViewById.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור לרכיבים בקובץ ה-XML (item_shift_employee)
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            btnRemove = itemView.findViewById(R.id.btnRemoveEmployee);
        }
    }
}