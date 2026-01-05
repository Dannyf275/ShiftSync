package com.example.shiftsync;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.Shift;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * מסך לוח המשמרות לעובד (Employee Schedule).
 * מאפשר צפייה במשמרות פנויות/משובצות לפי תאריך, ושליחת בקשות שיבוץ למנהל.
 */
public class EmployeeScheduleActivity extends AppCompatActivity {

    // אובייקטים לחיבור ל-Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // רכיבי התצוגה (UI)
    private RecyclerView rvShifts;      // הרשימה הנגללת של המשמרות
    private CalendarView calendarView;  // לוח השנה לבחירת תאריך
    private TextView tvDateTitle;       // כותרת המציגה את התאריך שנבחר

    // אדפטר ורשימה לניהול הנתונים בתצוגה
    private ShiftsAdapter adapter;
    private List<Shift> shiftsList;

    // משתנה לשמירת שם העובד הנוכחי (כדי לשמור את השם יחד עם ה-ID בעת בקשת משמרת)
    private String currentUserName = "Employee";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_schedule);

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // טעינת שם העובד מיד בעליית המסך (נדרש לשיבוץ)
        loadCurrentUserName();

        // קישור לרכיבים ב-XML
        rvShifts = findViewById(R.id.rvShifts);
        calendarView = findViewById(R.id.calendarView);
        tvDateTitle = findViewById(R.id.tvDateTitle);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // אתחול הרשימה והגדרת ה-RecyclerView
        shiftsList = new ArrayList<>();
        rvShifts.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר. אנו משתמשים באותו אדפטר של המנהל, אך מגדירים התנהגות שונה ללחיצות.
        adapter = new ShiftsAdapter(shiftsList, new ShiftsAdapter.OnShiftClickListener() {
            @Override
            public void onDeleteClick(int position) {
                // עובד לא מורשה למחוק משמרות מהמערכת
                Toast.makeText(EmployeeScheduleActivity.this, "אין הרשאה למחוק", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditClick(Shift shift) {
                // עובד לא מורשה לערוך פרטי משמרת
                Toast.makeText(EmployeeScheduleActivity.this, "אין הרשאה לערוך", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onShiftClick(Shift shift) {
                // לחיצה על גוף המשמרת -> פותחת דיאלוג להרשמה/ביטול
                handleShiftRegistration(shift);
            }
        });

        rvShifts.setAdapter(adapter);

        // הגדרת מאזין לשינוי תאריך בלוח השנה
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // יצירת אובייקט Calendar עם התאריך שנבחר
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);

            // טעינת המשמרות לתאריך החדש
            loadShiftsForDate(sel);
        });

        // טעינה ראשונית של משמרות להיום (בעת פתיחת המסך)
        loadShiftsForDate(Calendar.getInstance());

        // כפתור חזרה למסך הקודם
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * פונקציה ששולפת את שם המשתמש הנוכחי (FullName) מה-Firestore.
     * אנו צריכים את השם כדי שכאשר העובד מבקש משמרת, נשמור גם את ה-ID וגם את השם שלו
     * ברשימת הבקשות (Pending), כדי שהמנהל יראה מי ביקש.
     */
    private void loadCurrentUserName() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentUserName = doc.getString("fullName");
                        }
                    });
        }
    }

    /**
     * טעינת המשמרות עבור תאריך ספציפי.
     * הפונקציה מבצעת שאילתה ל-Firestore לפי טווח זמנים (מתחילת היום ועד סופו).
     * @param date - התאריך שנבחר בלוח השנה.
     */
    private void loadShiftsForDate(Calendar date) {
        // חישוב תחילת היום (00:00:00)
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        // חישוב סוף היום (23:59:59)
        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        // עדכון הכותרת בתצוגה
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDateTitle.setText("משמרות לתאריך: " + sdf.format(date.getTime()));

        // ביצוע השאילתה בזמן אמת (addSnapshotListener)
        // זה אומר שאם המנהל יוסיף משמרת בזמן שהעובד צופה במסך, היא תופיע מיד.
        db.collection("shifts")
                .whereGreaterThanOrEqualTo("startTime", startOfDay.getTimeInMillis()) // החל מ-00:00
                .whereLessThanOrEqualTo("startTime", endOfDay.getTimeInMillis())     // עד 23:59
                .addSnapshotListener((value, error) -> {
                    if (error != null) return; // טיפול בשגיאה

                    shiftsList.clear(); // ניקוי הרשימה הישנה

                    if (value != null) {
                        // המרת המסמכים שהתקבלו לאובייקטי Shift
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Shift shift = doc.toObject(Shift.class);
                            if (shift != null) shiftsList.add(shift);
                        }
                    }
                    // רענון התצוגה באדפטר
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * פונקציה שמחליטה איזה דיאלוג להציג לעובד בעת לחיצה על משמרת.
     * הלוגיקה:
     * 1. האם העובד כבר משובץ (Assigned)? -> הצע ביטול שיבוץ.
     * 2. האם העובד כבר שלח בקשה (Pending)? -> הצע ביטול בקשה.
     * 3. האם העובד לא רשום כלל? -> הצע שליחת בקשה חדשה.
     */
    private void handleShiftRegistration(Shift shift) {
        String uid = mAuth.getCurrentUser().getUid();

        // בדיקה האם ה-ID שלי נמצא ברשימת המשובצים
        boolean isAssigned = shift.getAssignedUserIds() != null && shift.getAssignedUserIds().contains(uid);
        // בדיקה האם ה-ID שלי נמצא ברשימת הממתינים
        boolean isPending = shift.getPendingUserIds() != null && shift.getPendingUserIds().contains(uid);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (isAssigned) {
            // מקרה 1: כבר משובץ
            builder.setTitle("ביטול שיבוץ");
            builder.setMessage("אתה משובץ למשמרת זו. האם לבטל?");
            builder.setPositiveButton("כן, בטל", (d, w) -> cancelAssignment(shift, uid));
        }
        else if (isPending) {
            // מקרה 2: ממתין לאישור
            builder.setTitle("ביטול בקשה");
            builder.setMessage("שלחת בקשה למשמרת זו. האם לבטל את הבקשה?");
            builder.setPositiveButton("כן, בטל", (d, w) -> cancelRequest(shift, uid));
        }
        else {
            // מקרה 3: לא רשום -> שליחת בקשה
            builder.setTitle("בקשה לשיבוץ");
            builder.setMessage("האם לשלוח בקשה למנהל להצטרף למשמרת?");
            builder.setPositiveButton("שלח בקשה", (d, w) -> sendRequestToShift(shift, uid));
        }

        builder.setNegativeButton("סגור", null);
        builder.show();
    }

    /**
     * שליחת בקשה למשמרת.
     * הפעולה: הוספת ה-ID והשם לרשימות ה-Pending ב-Firestore.
     * שימוש ב-arrayUnion מבטיח שלא יהיו כפילויות.
     */
    private void sendRequestToShift(Shift shift, String uid) {
        db.collection("shifts").document(shift.getShiftId())
                .update("pendingUserIds", FieldValue.arrayUnion(uid),
                        "pendingUserNames", FieldValue.arrayUnion(currentUserName))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "הבקשה נשלחה למנהל", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשליחת בקשה", Toast.LENGTH_SHORT).show());
    }

    /**
     * ביטול בקשה (לפני שאושרה).
     * הפעולה: הסרת ה-ID והשם מרשימות ה-Pending.
     */
    private void cancelRequest(Shift shift, String uid) {
        db.collection("shifts").document(shift.getShiftId())
                .update("pendingUserIds", FieldValue.arrayRemove(uid),
                        "pendingUserNames", FieldValue.arrayRemove(currentUserName))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "הבקשה בוטלה", Toast.LENGTH_SHORT).show());
    }

    /**
     * ביטול שיבוץ קיים.
     * הפעולה: הסרת ה-ID והשם מרשימות ה-Assigned.
     */
    private void cancelAssignment(Shift shift, String uid) {
        db.collection("shifts").document(shift.getShiftId())
                .update("assignedUserIds", FieldValue.arrayRemove(uid),
                        "assignedUserNames", FieldValue.arrayRemove(currentUserName))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "השיבוץ בוטל", Toast.LENGTH_SHORT).show());
    }
}