package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.User;

import java.util.List;

/**
 * מחלקה זו (Adapter) אחראית לחבר בין רשימת הנתונים (User) לבין התצוגה הגרפית (RecyclerView).
 * היא יוצרת את הכרטיסים הוויזואליים וממלאת אותם בתוכן.
 */
public class EmployeesAdapter extends RecyclerView.Adapter<EmployeesAdapter.EmployeeViewHolder> {

    // --- הגדרת הממשק (Interface) ---
    // הממשק הזה משמש כ"ערוץ תקשורת" החוצה ל-Activity.
    // כשיקרה משהו בתוך האדפטר (כמו לחיצה), אנחנו נודיע למי שמממש את הממשק הזה.
    public interface OnEmployeeClickListener {
        void onEditClick(User user); // פונקציה שתופעל כשלוחצים על "ערוך"
    }

    // --- משתנים (Fields) ---
    // ממוקמים בראש המחלקה לסדר וארגון.
    private List<User> employeesList;         // רשימת העובדים להצגה
    private OnEmployeeClickListener listener; // המאזין לאירועים

    // --- בנאי (Constructor) ---
    /**
     * בנאי יחיד ומחייב.
     * @param employeesList הרשימה שרוצים להציג.
     * @param listener הרכיב שיקבל את אירועי הלחיצה (בדרך כלל ה-Activity).
     */
    public EmployeesAdapter(List<User> employeesList, OnEmployeeClickListener listener) {
        this.employeesList = employeesList;
        this.listener = listener;
    }

    // --- מתודות של RecyclerView ---

    /**
     * שלב 1: יצירת ה"קליפה" הוויזואלית.
     * הפונקציה לוקחת את קובץ ה-XML (item_employee) ויוצרת ממנו אובייקט View בזיכרון.
     */
    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    /**
     * שלב 2: מילוי הנתונים.
     * הפונקציה רצה עבור כל שורה ומכניסה את המידע הנכון לשדות הטקסט.
     */
    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        // שליפת העובד הנוכחי לפי המיקום
        User employee = employeesList.get(position);

        // עדכון הטקסטים במסך
        holder.tvName.setText(employee.getFullName());
        holder.tvEmail.setText(employee.getEmail());

        // טיפול בשכר: המרה למחרוזת והוספת הסימן ₪
        holder.tvRate.setText(employee.getHourlyRate() + " ₪");

        // הערה: ניתן להוסיף כאן גם הצגה של ת"ז אם נרצה בעתיד
        // String idNum = (employee.getIdNumber() != null) ? employee.getIdNumber() : "";

        // הגדרת הלחיצה על כפתור "ערוך פרטים"
        holder.btnEdit.setOnClickListener(v -> {
            // בדיקת בטיחות: מוודאים שהמאזין קיים לפני שקוראים לו
            if (listener != null) {
                listener.onEditClick(employee);
            }
        });
    }

    /**
     * החזרת כמות הפריטים ברשימה.
     * הוספתי בדיקה האם הרשימה null כדי למנוע קריסה במקרה של אתחול שגוי.
     */
    @Override
    public int getItemCount() {
        if (employeesList == null) {
            return 0;
        }
        return employeesList.size();
    }

    // --- מחלקה פנימית (ViewHolder) ---
    /**
     * מחלקה שמחזיקה את האלמנטים של העיצוב (Views) עבור שורה אחת.
     * מונעת את הצורך לחפש אותם מחדש (findViewById) בכל גלילה.
     */
    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRate;
        Button btnEdit; // שימוש ב-Button רגיל של אנדרואיד

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור בין הקוד לרכיבים ב-XML
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmail = itemView.findViewById(R.id.tvEmployeeEmail);
            tvRate = itemView.findViewById(R.id.tvEmployeeRate);
            btnEdit = itemView.findViewById(R.id.btnEditEmployee);
        }
    }
}