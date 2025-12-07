package com.example.shiftsync;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shiftsync.databinding.ActivityManagerScheduleBinding;
import com.example.shiftsync.models.Shift;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * מסך ניהול סידור עבודה (למנהל בלבד).
 * במסך זה המנהל רואה לוח שנה ויזואלי, בוחר תאריך, ורואה את המשמרות לאותו יום.
 * המנהל יכול להוסיף משמרות חדשות או למחוק קיימות.
 */
public class ManagerScheduleActivity extends AppCompatActivity {

    // גישה לרכיבי העיצוב
    private ActivityManagerScheduleBinding binding;

    // חיבור למסד הנתונים
    private FirebaseFirestore db;

    // אדפטר לרשימת המשמרות
    private ShiftsAdapter adapter;

    // רשימת המשמרות בזיכרון
    private List<Shift> shiftsList;

    // משתנה ששומר את התאריך שנבחר כרגע בלוח השנה (ברירת מחדל: היום)
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. חיבור לעיצוב (ViewBinding)
        binding = ActivityManagerScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול משתנים
        db = FirebaseFirestore.getInstance();
        shiftsList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        // 3. הגדרת הרשימה (RecyclerView) ולוח השנה (CalendarView)
        setupRecyclerView();
        setupCalendar();

        // 4. טעינה ראשונית של המשמרות להיום
        loadShiftsForDate(selectedDate);

        // 5. הגדרת כפתורים
        binding.btnBack.setOnClickListener(v -> finish()); // חזרה למסך הראשי
        binding.fabAddShift.setOnClickListener(v -> showAddShiftDialog()); // הוספת משמרת
    }

    /**
     * הגדרת ה-RecyclerView והאדפטר.
     */
    private void setupRecyclerView() {
        binding.rvShifts.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר והגדרת מאזין למחיקה (Callback)
        // כאשר לוחצים על פח האשפה בשורה, הפונקציה deleteShift תופעל
        adapter = new ShiftsAdapter(shiftsList, position -> {
            Shift shiftToDelete = shiftsList.get(position);
            deleteShift(shiftToDelete);
        });

        binding.rvShifts.setAdapter(adapter);
    }

    /**
     * הגדרת המאזין לשינוי תאריך בלוח השנה.
     */
    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // עדכון המשתנה selectedDate לתאריך החדש שנבחר
            selectedDate.set(year, month, dayOfMonth);

            // טעינת הנתונים מחדש עבור התאריך שנבחר
            loadShiftsForDate(selectedDate);
        });
    }

    /**
     * פונקציה הטוענת את המשמרות מ-Firestore עבור תאריך ספציפי.
     * @param date התאריך שנבחר.
     */
    private void loadShiftsForDate(Calendar date) {
        // --- חישוב גבולות הזמן של היום ---
        // אנחנו רוצים לשלוף את כל המשמרות שמתחילות מ-00:00 ועד 23:59 של אותו יום.

        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        // עדכון הכותרת על המסך למשתמש (למשל: "משמרות לתאריך: 01/01/2025")
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.tvDateTitle.setText("משמרות לתאריך: " + sdf.format(date.getTime()));

        // --- שאילתה ל-Firestore ---
        // שליפת משמרות שזמן ההתחלה שלהן (startTime) נמצא בטווח שחישבנו.
        db.collection("shifts")
                .whereGreaterThanOrEqualTo("startTime", startOfDay.getTimeInMillis())
                .whereLessThanOrEqualTo("startTime", endOfDay.getTimeInMillis())
                .addSnapshotListener((value, error) -> {
                    // addSnapshotListener מאזין לשינויים בזמן אמת.
                    // אם נוסיף משמרת בדיאלוג, הרשימה תתעדכן לבד מיד.

                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    shiftsList.clear(); // ניקוי הרשימה הישנה
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Shift shift = doc.toObject(Shift.class);
                            if (shift != null) {
                                shiftsList.add(shift);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged(); // רענון התצוגה
                });
    }

    /**
     * הצגת דיאלוג להוספת משמרת חדשה.
     */
    private void showAddShiftDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // טעינת העיצוב של הדיאלוג
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shift, null);
        builder.setView(dialogView);

        // קישור לשדות שבתוך הדיאלוג
        EditText etStart = dialogView.findViewById(R.id.etDialogStartTime);
        EditText etEnd = dialogView.findViewById(R.id.etDialogEndTime);
        EditText etWorkers = dialogView.findViewById(R.id.etDialogRequiredWorkers);

        // יצירת עותקים של התאריך הנבחר כדי לשמור עליהם את השעות
        Calendar calStart = (Calendar) selectedDate.clone();
        Calendar calEnd = (Calendar) selectedDate.clone();

        // --- בחירת שעת התחלה ---
        etStart.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                calStart.set(Calendar.HOUR_OF_DAY, hour);
                calStart.set(Calendar.MINUTE, minute);
                etStart.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 8, 0, true).show(); // ברירת מחדל: 08:00
        });

        // --- בחירת שעת סיום ---
        etEnd.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                calEnd.set(Calendar.HOUR_OF_DAY, hour);
                calEnd.set(Calendar.MINUTE, minute);
                etEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 16, 0, true).show(); // ברירת מחדל: 16:00
        });

        // --- כפתור שמירה ---
        builder.setPositiveButton("שמור", (dialog, which) -> {
            String workersStr = etWorkers.getText().toString().trim();

            // ולידציה 1: האם הוזנה כמות עובדים?
            if (workersStr.isEmpty()) {
                Toast.makeText(this, "חובה להזין כמות עובדים", Toast.LENGTH_SHORT).show();
                return;
            }

            // ולידציה 2: האם שעת הסיום גדולה משעת ההתחלה?
            if (calEnd.getTimeInMillis() <= calStart.getTimeInMillis()) {
                Toast.makeText(this, "שעת הסיום חייבת להיות אחרי שעת ההתחלה", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                // המרת המחרוזת למספר
                int requiredWorkers = Integer.parseInt(workersStr);

                // יצירת אובייקט המשמרת החדש
                String id = UUID.randomUUID().toString();

                Shift newShift = new Shift(
                        id,
                        calStart.getTimeInMillis(),
                        calEnd.getTimeInMillis(),
                        requiredWorkers
                );

                // שמירה ב-Firestore
                db.collection("shifts").document(id).set(newShift)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "המשמרת נוספה!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } catch (NumberFormatException e) {
                Toast.makeText(this, "כמות עובדים חייבת להיות מספר", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    /**
     * פונקציה למחיקת משמרת (מופעלת דרך האדפטר).
     */
    private void deleteShift(Shift shift) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת משמרת")
                .setMessage("האם אתה בטוח שברצונך למחוק משמרת זו?")
                .setPositiveButton("כן", (dialog, which) -> {
                    db.collection("shifts").document(shift.getShiftId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "נמחק בהצלחה", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("לא", null)
                .show();
    }
}