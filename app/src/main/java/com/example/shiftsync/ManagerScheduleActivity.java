package com.example.shiftsync;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.databinding.ActivityManagerScheduleBinding;
import com.example.shiftsync.models.Shift;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * מסך ניהול הלו"ז (Manager Schedule).
 * זהו "מרכז הבקרה" של המנהל לניהול המשמרות.
 * כאן הוא קובע מתי עובדים, כמה עובדים צריך, ומנהל את השיבוצים בפועל.
 */
public class ManagerScheduleActivity extends AppCompatActivity {

    // קישור לרכיבי ה-XML
    private ActivityManagerScheduleBinding binding;

    // חיבור למסד הנתונים
    private FirebaseFirestore db;

    // אדפטר לרשימת המשמרות
    private ShiftsAdapter adapter;

    // רשימת המשמרות שמוצגת כרגע על המסך
    private List<Shift> shiftsList;

    // התאריך שנבחר בלוח השנה (ברירת מחדל: היום)
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagerScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // אתחול משתנים
        db = FirebaseFirestore.getInstance();
        shiftsList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        // הגדרת הרשימה והאדפטר
        setupRecyclerView();

        // הגדרת מאזין ללוח השנה
        setupCalendar();

        // טעינה ראשונית של המשמרות להיום
        loadShiftsForDate(selectedDate);

        // כפתור חזרה
        binding.btnBack.setOnClickListener(v -> finish());

        // כפתור הוספת משמרת חדשה (+)
        binding.fabAddShift.setOnClickListener(v -> showAddShiftDialog());
    }

    /**
     * הגדרת ה-RecyclerView שמציג את רשימת המשמרות.
     * כאן אנו מגדירים מה קורה בכל סוג של לחיצה (מחיקה, עריכה, או לחיצה רגילה).
     */
    private void setupRecyclerView() {
        binding.rvShifts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ShiftsAdapter(shiftsList, new ShiftsAdapter.OnShiftClickListener() {
            @Override
            public void onDeleteClick(int position) {
                // לחיצה על פח האשפה -> מחיקת המשמרת
                deleteShift(shiftsList.get(position));
            }

            @Override
            public void onEditClick(Shift shift) {
                // לחיצה על העיפרון -> עריכת פרטי המשמרת
                showEditShiftDialog(shift);
            }

            @Override
            public void onShiftClick(Shift shift) {
                // לחיצה על גוף המשמרת -> הצגת העובדים המשובצים בה (וניהול שלהם)
                showShiftEmployeesDialog(shift);
            }
        });

        binding.rvShifts.setAdapter(adapter);
    }

    /**
     * פונקציה להצגת העובדים המשובצים למשמרת ספציפית.
     * נפתחת בדיאלוג ומאפשרת למנהל להסיר עובד מהמשמרת.
     */
    private void showShiftEmployeesDialog(Shift shift) {
        // שלב 1: הכנת הנתונים לאדפטר הפנימי.
        // האובייקט Shift מחזיק שתי רשימות נפרדות (IDs ו-Names).
        // אנחנו מאחדים אותן לרשימה אחת של אובייקטים מסוג EmployeeItem כדי שיהיה קל להציג.
        List<ShiftEmployeesAdapter.EmployeeItem> employees = new ArrayList<>();

        if (shift.getAssignedUserIds() != null && !shift.getAssignedUserIds().isEmpty()) {
            for (int i = 0; i < shift.getAssignedUserIds().size(); i++) {
                String id = shift.getAssignedUserIds().get(i);
                String name = "עובד"; // שם ברירת מחדל למקרה של אי-תאימות

                // שליפת השם מהרשימה המקבילה (אם קיים)
                if (shift.getAssignedUserNames() != null && shift.getAssignedUserNames().size() > i) {
                    name = shift.getAssignedUserNames().get(i);
                }
                employees.add(new ShiftEmployeesAdapter.EmployeeItem(id, name));
            }
        }

        // שלב 2: יצירת הדיאלוג (Pop-up window)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // טעינת העיצוב המיוחד לדיאלוג הזה
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_shift_employees, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // שלב 3: הגדרת ה-RecyclerView הפנימי (שבתוך הדיאלוג)
        RecyclerView rv = view.findViewById(R.id.rvShiftEmployees);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // שימוש באדפטר הייעודי שיצרנו (ShiftEmployeesAdapter)
        ShiftEmployeesAdapter dialogAdapter = new ShiftEmployeesAdapter(employees, itemToRemove -> {
            // הגדרת הפעולה בעת לחיצה על "הסר" (X) ליד שם עובד
            removeEmployeeFromShift(shift, itemToRemove, dialog);
        });
        rv.setAdapter(dialogAdapter);

        // כפתור סגירה לדיאלוג
        Button btnClose = view.findViewById(R.id.btnCloseDialog);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * הסרת עובד ממשמרת.
     * שימוש בפקודת arrayRemove של Firestore כדי למחוק את העובד מהרשימות בצורה בטוחה.
     */
    private void removeEmployeeFromShift(Shift shift, ShiftEmployeesAdapter.EmployeeItem item, AlertDialog dialog) {
        db.collection("shifts").document(shift.getShiftId())
                .update("assignedUserIds", FieldValue.arrayRemove(item.id),
                        "assignedUserNames", FieldValue.arrayRemove(item.name))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "העובד הוסר בהצלחה", Toast.LENGTH_SHORT).show();
                    dialog.dismiss(); // סגירת הדיאלוג כדי לרענן את הנתונים ברקע
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בהסרה", Toast.LENGTH_SHORT).show());
    }

    /**
     * הגדרת לוח השנה.
     * בכל פעם שבוחרים יום אחר, מעדכנים את המשתנה selectedDate וטוענים מחדש.
     */
    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            loadShiftsForDate(selectedDate);
        });
    }

    /**
     * טעינת משמרות לפי טווח תאריכים (מתחילת היום ועד סופו).
     */
    private void loadShiftsForDate(Calendar date) {
        // חישוב 00:00:00
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        // חישוב 23:59:59
        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        // עדכון הכותרת
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.tvDateTitle.setText("משמרות לתאריך: " + sdf.format(date.getTime()));

        // ביצוע השאילתה
        db.collection("shifts")
                .whereGreaterThanOrEqualTo("startTime", startOfDay.getTimeInMillis())
                .whereLessThanOrEqualTo("startTime", endOfDay.getTimeInMillis())
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    shiftsList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Shift shift = doc.toObject(Shift.class);
                            if (shift != null) shiftsList.add(shift);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * הצגת דיאלוג להוספת משמרת חדשה.
     * כולל בחירת שעות, כמות עובדים והערות.
     */
    private void showAddShiftDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shift, null);
        builder.setView(dialogView);

        // קישור לשדות
        EditText etStart = dialogView.findViewById(R.id.etDialogStartTime);
        EditText etEnd = dialogView.findViewById(R.id.etDialogEndTime);
        EditText etWorkers = dialogView.findViewById(R.id.etDialogRequiredWorkers);
        EditText etNotes = dialogView.findViewById(R.id.etDialogNotes); // שדה ההערות החדש

        // יצירת אובייקטי Calendar לשמירת השעות שנבחרו
        Calendar calStart = (Calendar) selectedDate.clone();
        Calendar calEnd = (Calendar) selectedDate.clone();

        // הגדרת בורר השעות (TimePicker) לשעת התחלה
        etStart.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, minute) -> {
            calStart.set(Calendar.HOUR_OF_DAY, hour); calStart.set(Calendar.MINUTE, minute);
            etStart.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        }, 8, 0, true).show());

        // הגדרת בורר השעות לשעת סיום
        etEnd.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, minute) -> {
            calEnd.set(Calendar.HOUR_OF_DAY, hour); calEnd.set(Calendar.MINUTE, minute);
            etEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        }, 16, 0, true).show());

        builder.setPositiveButton("שמור", (d, w) -> {
            try {
                // המרת הקלט וניקוי
                int req = Integer.parseInt(etWorkers.getText().toString());
                String notes = etNotes.getText().toString();

                // יצירת ID ייחודי
                String id = UUID.randomUUID().toString();

                // יצירת אובייקט Shift ושמירה ב-Firestore
                Shift s = new Shift(id, calStart.getTimeInMillis(), calEnd.getTimeInMillis(), req, notes);
                db.collection("shifts").document(id).set(s)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "המשמרת נוצרה", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Toast.makeText(this, "נא למלא את כל השדות בצורה תקינה", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    /**
     * הצגת דיאלוג לעריכת משמרת קיימת.
     * דומה מאוד להוספה, אבל ממלא את השדות בערכים הקיימים ומבצע Update במקום Set.
     */
    private void showEditShiftDialog(Shift shift) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shift, null);
        builder.setView(dialogView);

        EditText etStart = dialogView.findViewById(R.id.etDialogStartTime);
        EditText etEnd = dialogView.findViewById(R.id.etDialogEndTime);
        EditText etWorkers = dialogView.findViewById(R.id.etDialogRequiredWorkers);
        EditText etNotes = dialogView.findViewById(R.id.etDialogNotes);

        // מילוי הנתונים הקיימים
        Calendar calStart = Calendar.getInstance(); calStart.setTimeInMillis(shift.getStartTime());
        Calendar calEnd = Calendar.getInstance(); calEnd.setTimeInMillis(shift.getEndTime());

        etStart.setText(String.format(Locale.getDefault(), "%02d:%02d", calStart.get(Calendar.HOUR_OF_DAY), calStart.get(Calendar.MINUTE)));
        etEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", calEnd.get(Calendar.HOUR_OF_DAY), calEnd.get(Calendar.MINUTE)));
        etWorkers.setText(String.valueOf(shift.getRequiredWorkers()));
        etNotes.setText(shift.getNotes());

        // הגדרת בוררי השעות מחדש (למקרה שרוצים לשנות)
        etStart.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, minute) -> {
            calStart.set(Calendar.HOUR_OF_DAY, hour); calStart.set(Calendar.MINUTE, minute);
            etStart.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        }, 8, 0, true).show());

        etEnd.setOnClickListener(v -> new TimePickerDialog(this, (view, hour, minute) -> {
            calEnd.set(Calendar.HOUR_OF_DAY, hour); calEnd.set(Calendar.MINUTE, minute);
            etEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        }, 16, 0, true).show());

        builder.setPositiveButton("עדכן", (d, w) -> {
            try {
                int req = Integer.parseInt(etWorkers.getText().toString());
                String notes = etNotes.getText().toString();

                // יצירת מפה (Map) עם השדות שרוצים לעדכן בלבד
                Map<String, Object> updates = new HashMap<>();
                updates.put("startTime", calStart.getTimeInMillis());
                updates.put("endTime", calEnd.getTimeInMillis());
                updates.put("requiredWorkers", req);
                updates.put("notes", notes); // עדכון ההערות

                db.collection("shifts").document(shift.getShiftId()).update(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "המשמרת עודכנה", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Toast.makeText(this, "שגיאה בעדכון", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    /**
     * מחיקת משמרת.
     */
    private void deleteShift(Shift shift) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקה")
                .setMessage("למחוק את המשמרת?")
                .setPositiveButton("כן", (d,w)-> db.collection("shifts").document(shift.getShiftId()).delete())
                .setNegativeButton("לא", null)
                .show();
    }
}