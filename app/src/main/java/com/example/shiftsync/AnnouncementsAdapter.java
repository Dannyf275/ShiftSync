package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shiftsync.models.Announcement;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * מחלקה האחראית על ניהול רשימת ההודעות בתוך ה-RecyclerView.
 * היא מקבלת רשימה של אובייקטים מסוג Announcement ומציגה אותם על המסך.
 */
public class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder> {

    /**
     * ממשק (Interface) להעברת אירועי לחיצה חזרה ל-Activity.
     * למה זה נחוץ? האדפטר אחראי רק על התצוגה. הוא לא יודע למחוק ממסד הנתונים.
     * כשיש לחיצה על "מחק", האדפטר מודיע ל-Activity דרך הממשק הזה, וה-Activity מבצע את המחיקה בפועל.
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(Announcement announcement);
    }

    // רשימת ההודעות להצגה (מגיעה מ-Firebase דרך ה-Activity).
    private List<Announcement> list;

    // המאזין ללחיצות. אם הוא null, סימן שאנחנו במצב צפייה בלבד (למשל אצל עובד).
    private OnDeleteClickListener listener;

    /**
     * בנאי (Constructor).
     * @param list - הרשימה של ההודעות שרוצים להציג.
     * @param listener - מי שמטפל במחיקה. אם מעבירים כאן null, כפתורי המחיקה יוסתרו.
     */
    public AnnouncementsAdapter(List<Announcement> list, OnDeleteClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    /**
     * פונקציה שנקראת כשה-RecyclerView צריך ליצור שורה חדשה בזיכרון.
     * היא "מנפחת" (Inflates) את קובץ ה-XML של השורה (item_announcement).
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינת קובץ העיצוב item_announcement.xml והמרתו לאובייקט View ב-Java.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        // יצירת ViewHolder שעוטף את ה-View הזה ושומר הפניות לרכיבים שלו.
        return new ViewHolder(view);
    }

    /**
     * פונקציה שנקראת כדי "לחבר" (Bind) את הנתונים לשורה ספציפית.
     * כאן אנחנו לוקחים את המידע מהרשימה ושמים אותו בתוך הטקסטים והכפתורים.
     * @param holder - המחזיק של הרכיבים הגרפיים.
     * @param position - המיקום של השורה ברשימה (0, 1, 2...).
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 1. שליפת האובייקט הספציפי מהרשימה לפי המיקום הנוכחי.
        Announcement item = list.get(position);

        // 2. הצגת הנתונים הבסיסיים (כותרת ותוכן) בתוך ה-TextViews.
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());

        // 3. המרת זמן (Timestamp long) לתאריך קריא (String).
        // הפורמט הוא: יום/חודש/שנה שעה:דקה (למשל: 01/01/2025 10:30).
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(item.getTimestamp()));

        // 4. הצגת שם המנהל שכתב את ההודעה.
        holder.tvAuthor.setText("מאת: " + item.getAuthorName());

        // 5. לוגיקת כפתור המחיקה:
        // אם הועבר listener (כלומר אנחנו במסך ניהול מודעות של מנהל):
        if (listener != null) {
            holder.btnDelete.setVisibility(View.VISIBLE); // מציגים את הכפתור
            // מגדירים מה קורה בלחיצה: מפעילים את הפונקציה בממשק ומעבירים את הפריט הנוכחי.
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
        } else {
            // אם ה-listener הוא null (אנחנו אצל עובד או במצב צפייה בלבד):
            holder.btnDelete.setVisibility(View.GONE); // מסתירים את הכפתור לחלוטין (לא תופס מקום).
        }
    }

    /**
     * מחזירה את מספר הפריטים ברשימה.
     * ה-RecyclerView חייב את זה כדי לדעת כמה שורות לצייר.
     */
    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    /**
     * מחלקה פנימית (Inner Class) מסוג ViewHolder.
     * התפקיד שלה הוא לשמור את ההפניות (References) לרכיבים בתוך ה-XML (כמו TextViews).
     * זה משפר ביצועים כי לא צריך לעשות findViewById כל פעם שגוללים את המסך.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvDate, tvAuthor;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור בין המשתנים בקוד למזהים (IDs) בקובץ item_announcement.xml
            tvTitle = itemView.findViewById(R.id.tvAnnTitle);
            tvContent = itemView.findViewById(R.id.tvAnnContent);
            tvDate = itemView.findViewById(R.id.tvAnnDate);
            tvAuthor = itemView.findViewById(R.id.tvAnnAuthor);
            btnDelete = itemView.findViewById(R.id.btnDeleteAnn);
        }
    }
}