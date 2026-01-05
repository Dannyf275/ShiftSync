package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shiftsync.models.Shift;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * אדפטר לטבלת השכר (Salary Table Adapter).
 * אחראי להציג כל משמרת כשורה בודדת בדו"ח השכר במסך האפליקציה.
 * האדפטר מבצע חישוב מקומי של השעות והסכום עבור כל משמרת.
 */
public class SalaryAdapter extends RecyclerView.Adapter<SalaryAdapter.ViewHolder> {

    // רשימת המשמרות שבוצעו החודש
    private List<Shift> shifts;

    // התעריף השעתי של העובד (מועבר מה-Activity כדי לחשב את הסכום)
    private double hourlyRate;

    /**
     * בנאי (Constructor).
     * @param shifts - רשימת המשמרות להצגה.
     * @param hourlyRate - השכר השעתי של העובד (לצורך חישוב עמודת "סכום").
     */
    public SalaryAdapter(List<Shift> shifts, double hourlyRate) {
        this.shifts = shifts;
        this.hourlyRate = hourlyRate;
    }

    /**
     * יצירת המראה הוויזואלי של שורה בטבלה.
     * מנפח את הקובץ item_salary_shift.xml.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינת קובץ העיצוב XML של השורה
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_salary_shift, parent, false);
        return new ViewHolder(view);
    }

    /**
     * חיבור הנתונים לשורה ספציפית (Binding).
     * כאן מתבצע החישוב של משך המשמרת והסכום לתשלום עבורה.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 1. שליפת המשמרת הנוכחית
        Shift shift = shifts.get(position);

        // 2. חישוב משך הזמן (Duration)
        // זמן סיום פחות זמן התחלה (במילי-שניות)
        long durationMillis = shift.getEndTime() - shift.getStartTime();

        // המרה לשעות (כולל שבר עשרוני, למשל 8.5 שעות)
        double hours = (double) durationMillis / (1000 * 60 * 60);

        // 3. חישוב הסכום הכספי למשמרת זו
        double amount = hours * hourlyRate;

        // 4. עיצוב תאריך להצגה (יום/חודש/שנה)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // 5. עדכון הטקסטים בתצוגה
        holder.tvDate.setText(sdf.format(shift.getStartTime())); // תאריך
        holder.tvHours.setText(String.format(Locale.getDefault(), "%.1f שעות", hours)); // שעות (עם ספרה אחת אחרי הנקודה)
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₪%.2f", amount)); // סכום (עם שתי ספרות ומטבע)
    }

    /**
     * כמות השורות בטבלה.
     */
    @Override
    public int getItemCount() {
        return shifts.size();
    }

    /**
     * מחלקת ViewHolder - שומרת הפניות לרכיבי הטקסט בשורה.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvHours, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור ל-IDs בקובץ ה-XML (item_salary_shift)
            tvDate = itemView.findViewById(R.id.tvDate);
            tvHours = itemView.findViewById(R.id.tvHours);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}