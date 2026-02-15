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

//הצגת ההודעה בrecyclerview
public class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder> {

    //מאזין ללחיצה
    public interface OnDeleteClickListener {
        void onDeleteClick(Announcement announcement);
    }

    // רשימת ההודעות להצגה
    private List<Announcement> list;

    private OnDeleteClickListener listener;

    //בנאי
    public AnnouncementsAdapter(List<Announcement> list, OnDeleteClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    //מימוש הrecyclerview במבנה קבוע כפי שנלמד
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינת קובץ העיצוב item_announcement.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        // יצירת ViewHolder
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // שליפת האובייקט הספציפי מהרשימה לפי המיקום
        Announcement item = list.get(position);

        // הצגת הנתונים הבסיסיים
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());

        //  המרת זמן לתאריך קריא (String)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(item.getTimestamp()));

        //  הצגת שם המנהל שכתב את ההודעה
        holder.tvAuthor.setText("מאת: " + item.getAuthorName());

        // כפתור המחיקה
        // אם הועבר listener
        if (listener != null) {
            holder.btnDelete.setVisibility(View.VISIBLE); // מציגים את הכפתור
            // מגדירים מה קורה בלחיצה: מפעילים את הפונקציה בממשק ומעבירים את הפריט הנוכחי.
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
        } else {
            // אם ה-listener הוא null (אנחנו אצל עובד או במצב צפייה בלבד):
            holder.btnDelete.setVisibility(View.GONE); // מסתירים את הכפתור לחלוטין (לא תופס מקום).
        }
    }

    //החזרת מספר פריטים ברשימה לטעינה של הrecyclerview
    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    //viewholder לXML
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvDate, tvAuthor;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור בין המשתנים בקוד למזהים בקובץ item_announcement.xml
            tvTitle = itemView.findViewById(R.id.tvAnnTitle);
            tvContent = itemView.findViewById(R.id.tvAnnContent);
            tvDate = itemView.findViewById(R.id.tvAnnDate);
            tvAuthor = itemView.findViewById(R.id.tvAnnAuthor);
            btnDelete = itemView.findViewById(R.id.btnDeleteAnn);
        }
    }
}