package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shiftsync.models.User;
import java.util.List;

/**
 * אדפטר (Adapter) לרשימת העובדים.
 * תפקידו לקשר בין רשימת העובדים (List<User>) לבין הרכיב הגרפי שמציג אותם (RecyclerView).
 * הוא יוצר את התצוגה לכל שורה וממלא אותה בתוכן הרלוונטי.
 */
public class EmployeesAdapter extends RecyclerView.Adapter<EmployeesAdapter.ViewHolder> {

    /**
     * ממשק (Interface) להעברת אירועי לחיצה חזרה ל-Activity.
     * האדפטר עצמו לא יודע "למחוק עובד" או "לפתוח דיאלוג עריכה", הוא רק יודע שהמשתמש לחץ על הכפתור.
     * ה-Activity (שמממש את הממשק הזה) יבצע את הלוגיקה בפועל מול Firebase.
     */
    public interface OnEmployeeClickListener {
        void onDeleteClick(User user); // לחיצה על פח אשפה
        void onEditClick(User user);   // לחיצה על עיפרון (עריכת שכר)
    }

    // רשימת העובדים שמוצגת כרגע על המסך
    private List<User> employees;

    // המאזין לאירועים (ה-Activity)
    private OnEmployeeClickListener listener;

    /**
     * בנאי (Constructor).
     * @param employees - הרשימה ההתחלתית.
     * @param listener - מי שיטפל בלחיצות הכפתורים.
     */
    public EmployeesAdapter(List<User> employees, OnEmployeeClickListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    /**
     * פונקציה לעדכון הרשימה (למשל בעת חיפוש/סינון).
     * כשאנחנו מקלידים בתיבת החיפוש, אנחנו שולחים לכאן רשימה מסוננת,
     * והפונקציה מרעננת את התצוגה.
     */
    public void updateList(List<User> newList) {
        this.employees = newList;
        notifyDataSetChanged(); // פקודה ל-RecyclerView לצייר מחדש את הכל
    }

    /**
     * יצירת המראה הוויזואלי של שורה אחת (ViewHolder).
     * הפונקציה "מנפחת" (Inflate) את קובץ ה-XML שיצרנו (item_employee.xml).
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }

    /**
     * חיבור הנתונים לשורה ספציפית (Binding).
     * כאן אנחנו לוקחים את פרטי העובד מהרשימה ושמים אותם בתוך ה-TextViews וה-ImageView.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = employees.get(position);

        // 1. הצגת טקסטים
        holder.tvName.setText(user.getFullName());
        holder.tvId.setText("ת.ז: " + user.getIdNumber());
        holder.tvRate.setText("שכר שעתי: " + user.getHourlyRate());

        // 2. טיפול בתמונת פרופיל
        // אם למשתמש יש תמונה שמורה (Base64), נמיר אותה לתמונה אמיתית (Bitmap) ונציג.
        // אחרת - נציג תמונת ברירת מחדל (אייקון עגול).
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            holder.ivProfile.setImageBitmap(ImageUtils.stringToBitmap(user.getProfileImage()));
        } else {
            holder.ivProfile.setImageResource(R.mipmap.ic_launcher_round);
        }

        // 3. הגדרת כפתורי הפעולה (עריכה ומחיקה)
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(user));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(user));
    }

    /**
     * כמה פריטים יש ברשימה?
     */
    @Override
    public int getItemCount() {
        return employees == null ? 0 : employees.size();
    }

    /**
     * מחלקת ViewHolder - מחזיקה את ההפניות לרכיבים הגרפיים.
     * זה מונע מאיתנו לחפש את הרכיבים (findViewById) שוב ושוב בכל גלילה.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // המשתנים חייבים להתאים לסוגים בקובץ item_employee.xml
        TextView tvName, tvId, tvRate;
        ImageButton btnDelete, btnEdit; // שימוש ב-ImageButton כי ב-XML הגדרנו אייקונים
        ImageView ivProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור ה-Java ל-XML לפי ה-IDs שהגדרנו
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvId = itemView.findViewById(R.id.tvEmployeeId);
            tvRate = itemView.findViewById(R.id.tvEmployeeRate);

            btnDelete = itemView.findViewById(R.id.btnDeleteEmployee);
            btnEdit = itemView.findViewById(R.id.btnEditEmployee);

            ivProfile = itemView.findViewById(R.id.ivEmployeeProfile);
        }
    }
}