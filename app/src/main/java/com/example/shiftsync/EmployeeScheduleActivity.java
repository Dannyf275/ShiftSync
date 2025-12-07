package com.example.shiftsync;

import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

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

public class EmployeeScheduleActivity extends AppCompatActivity {

    private ActivityEmployeeScheduleBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EmployeeShiftsAdapter adapter;
    private List<Shift> shiftsList;
    private Calendar selectedDate;

    // משתנה לשמירת שם העובד הנוכחי
    private String currentUserName = "עובד";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeeScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        shiftsList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        // 1. שליפת שם העובד מיד בכניסה למסך
        fetchCurrentUserName();

        binding.tvDateTitle.setText("בחר תאריך לצפייה והרשמה");

        setupRecyclerView();
        setupCalendar();
        loadShiftsForDate(selectedDate);

        binding.btnBack.setOnClickListener(v -> finish());
    }

    /**
     * שליפת שם המשתמש הנוכחי מ-Firestore.
     * נחוץ כדי שכאשר העובד יבקש משמרת, נשמור גם את שמו עבור המנהל.
     */
    private void fetchCurrentUserName() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getString("fullName") != null) {
                currentUserName = doc.getString("fullName");
            }
        });
    }

    private void setupRecyclerView() {
        binding.rvShifts.setLayoutManager(new LinearLayoutManager(this));
        String currentUid = mAuth.getCurrentUser().getUid();

        adapter = new EmployeeShiftsAdapter(shiftsList, currentUid, new EmployeeShiftsAdapter.OnShiftActionListener() {
            @Override
            public void onSignUp(Shift shift) {
                requestShift(shift); // שינינו את שם הפונקציה ל-requestShift
            }

            @Override
            public void onCancel(Shift shift) {
                cancelRequest(shift);
            }
        });

        binding.rvShifts.setAdapter(adapter);
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            loadShiftsForDate(selectedDate);
        });
    }

    private void loadShiftsForDate(Calendar date) {
        // ... (אותו קוד חישוב תאריכים כמו קודם) ...
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.tvDateTitle.setText("משמרות לתאריך: " + sdf.format(date.getTime()));

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

    // --- לוגיקה מעודכנת להרשמה (Pending) ---

    private void requestShift(Shift shift) {
        String uid = mAuth.getCurrentUser().getUid();

        // הגנה: אם השם טרם נטען, נשתמש במייל או בערך ברירת מחדל
        if (currentUserName == null || currentUserName.isEmpty()) {
            currentUserName = mAuth.getCurrentUser().getEmail();
        }

        Log.d("SHIFT_DEBUG", "Requesting shift: " + shift.getShiftId() + " for user: " + currentUserName);

        db.collection("shifts").document(shift.getShiftId())
                .update(
                        "pendingUserIds", FieldValue.arrayUnion(uid),
                        "pendingUserNames", FieldValue.arrayUnion(currentUserName)
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d("SHIFT_DEBUG", "Success! Request sent.");
                    Toast.makeText(this, "הבקשה נשלחה לאישור מנהל", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("SHIFT_DEBUG", "Error sending request: " + e.getMessage());
                    Toast.makeText(this, "שגיאה בשליחת בקשה", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelRequest(Shift shift) {
        String uid = mAuth.getCurrentUser().getUid();

        // ביטול יכול להיות משני מצבים: או שאני בהמתנה, או שאני כבר משובץ
        // לצורך הפשטות ננסה להסיר משתי הרשימות (לא מזיק אם לא קיים)
        db.collection("shifts").document(shift.getShiftId())
                .update(
                        "pendingUserIds", FieldValue.arrayRemove(uid),
                        "pendingUserNames", FieldValue.arrayRemove(currentUserName),
                        "assignedUserIds", FieldValue.arrayRemove(uid),
                        "assignedUserNames", FieldValue.arrayRemove(currentUserName)
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "הבקשה/השיבוץ בוטל", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בביטול", Toast.LENGTH_SHORT).show());
    }
}