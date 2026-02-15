package com.example.shiftsync;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shiftsync.databinding.ActivityEmployeeScheduleBinding;
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

//לוח משמרות עובד
public class EmployeeScheduleActivity extends AppCompatActivity {

    private ActivityEmployeeScheduleBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // שימוש באדפטר הייעודי לעובד
    private EmployeeShiftsAdapter adapter;

    private List<Shift> shiftsList;
    private Calendar selectedDate;
    private String currentUserId;
    private String currentUserName = "Employee"; // שם ברירת מחדל עד לטעינה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeeScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // אתחול פיירבייס
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // בדיקה למניעת קריסה אם אין משתמש מחובר
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        shiftsList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        // טעינת שם העובד מבסיס הנתונים
        loadUserName();

        // הגדרת הרשימה והאדפטר
        setupRecyclerView();

        // הגדרת לוח השנה
        setupCalendar();

        // טעינה ראשונית של משמרות להיום
        loadShiftsForDate(selectedDate);

        // כפתור חזרה
        binding.btnBack.setOnClickListener(v -> finish());
    }

    //טעינת משתמש נוכחי
    private void loadUserName() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getString("fullName") != null) {
                currentUserName = doc.getString("fullName");
            }
        });
    }

    //הגדרת recyclerview ואדפטר
    private void setupRecyclerView() {
        binding.rvShifts.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר עם הממשק לטיפול בלחיצות
        adapter = new EmployeeShiftsAdapter(shiftsList, currentUserId, new EmployeeShiftsAdapter.OnShiftActionListener() {
            @Override
            public void onSignUp(Shift shift) {
                // הרשמה למשמרת
                signUpForShift(shift);
            }

            @Override
            public void onCancel(Shift shift) {
                // ביטול הרשמה
                cancelSignUp(shift);
            }
        });

        binding.rvShifts.setAdapter(adapter);
    }

    //מאזין ללוח השנה
    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // עדכון התאריך הנבחר
            selectedDate.set(year, month, dayOfMonth);
            // טעינת המשמרות מחדש לתאריך זה
            loadShiftsForDate(selectedDate);
        });
    }

    //טעינת משמרות לפי תאריך
    private void loadShiftsForDate(Calendar date) {
        // חישוב תחילת היום (00:00) וסוף היום (23:59)
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0); startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23); endOfDay.set(Calendar.MINUTE, 59); endOfDay.set(Calendar.SECOND, 59);

        // עדכון כותרת הטקסט במסך
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.tvDateTitle.setText("משמרות לתאריך: " + sdf.format(date.getTime()));

        // יבוא משמרות בטווח המבוקש מהפיירבייס
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
                    // רענון התצוגה באדפטר
                    adapter.notifyDataSetChanged();
                });
    }


     //ביצוע הרשמה למשמרת (הוספה לרשימת הממתינים)

    private void signUpForShift(Shift shift) {
        db.collection("shifts").document(shift.getShiftId())
                .update(
                        "pendingUserIds", FieldValue.arrayUnion(currentUserId),
                        "pendingUserNames", FieldValue.arrayUnion(currentUserName)
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "בקשה נשלחה למנהל", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשליחת בקשה", Toast.LENGTH_SHORT).show());
    }


     //ביטול הרשמה

    private void cancelSignUp(Shift shift) {
        db.collection("shifts").document(shift.getShiftId())
                .update(
                        "pendingUserIds", FieldValue.arrayRemove(currentUserId),
                        "pendingUserNames", FieldValue.arrayRemove(currentUserName),
                        "assignedUserIds", FieldValue.arrayRemove(currentUserId),
                        "assignedUserNames", FieldValue.arrayRemove(currentUserName)
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "ההרשמה בוטלה", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בביטול", Toast.LENGTH_SHORT).show());
    }
}