package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityEmployeeBinding;
import com.example.shiftsync.models.Shift;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // חשוב!
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * הדשבורד הראשי של העובד.
 * מציג נתונים חיים (Live Data) אודות המשמרת הקרובה והשכר.
 */
public class EmployeeActivity extends AppCompatActivity {

    private ActivityEmployeeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // משתנה עזר לחישוב השכר
    private double hourlyRate = 0.0;

    // משתנה להחזקת המאזין החי של "המשמרת הבאה"
    private ListenerRegistration nextShiftListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול Binding
        binding = ActivityEmployeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. טעינת נתונים ראשונית (אם מחובר)
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            // טעינת שם ותעריף (קריאה רגילה כי זה לא משתנה הרבה)
            loadEmployeeDetails(uid);

            // חישוב שכר (קריאה רגילה כי זה חישוב כבד)
            calculateMonthlySalary(uid);

            // **האזנה חיה** למשמרת הבאה (כי זה קריטי שיהיה מעודכן)
            startListeningForNextShift(uid);
        }

        // 4. הגדרת כפתורים
        setupButtons();
    }

    // --- מחזור חיים ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // מחיקת המאזין כשהאקטיביטי נהרס (למשל ביציאה מהאפליקציה)
        if (nextShiftListener != null) {
            nextShiftListener.remove();
        }
    }

    /**
     * פונקציה לרענון כל הנתונים (מופעלת בלחיצה על כפתור הסנכרון).
     */
    private void refreshAllData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        loadEmployeeDetails(uid);
        calculateMonthlySalary(uid);
        // אין צורך לקרוא שוב ל-startListeningForNextShift כי הוא כבר רץ ברקע
    }

    /**
     * טעינת פרטי המשתמש (שם + תעריף שכר).
     */
    private void loadEmployeeDetails(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.tvWelcomeTitle.setText("שלום, " + user.getFullName());
                            hourlyRate = user.getHourlyRate();

                            // ברגע שיש לנו תעריף מעודכן, נחשב שוב את השכר
                            calculateMonthlySalary(uid);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show());
    }

    /**
     * האזנה בזמן אמת למשמרת הקרובה ביותר.
     * ברגע שמנהל מאשר משמרת, הפונקציה הזו תקפוץ ותעדכן את המסך.
     */
    private void startListeningForNextShift(String uid) {
        long now = System.currentTimeMillis();

        // שאילתה מורכבת:
        // 1. משמרות עתידיות (startTime > now)
        // 2. שאני משובץ בהן (assignedUserIds contains uid)
        // 3. ממוינות לפי זמן עולה (הכי קרובה ראשונה)
        // 4. מוגבלות לתוצאה אחת בלבד

        nextShiftListener = db.collection("shifts")
                .whereGreaterThan("startTime", now)
                .whereArrayContains("assignedUserIds", uid)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // שגיאה (קורה לפעמים אם חסר אינדקס ב-Firebase)
                        binding.tvNextShiftDate.setText("שגיאה בטעינה");
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        // יש משמרת קרובה!
                        Shift nextShift = value.getDocuments().get(0).toObject(Shift.class);
                        if (nextShift != null) {
                            SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

                            binding.tvNextShiftDate.setText(dateFmt.format(nextShift.getStartTime()));
                            binding.tvNextShiftTime.setText(timeFmt.format(nextShift.getStartTime()) + " - " + timeFmt.format(nextShift.getEndTime()));
                        }
                    } else {
                        // אין משמרות עתידיות מאושרות
                        binding.tvNextShiftDate.setText("אין משמרות קרובות");
                        binding.tvNextShiftTime.setText("--:-- - --:--");
                    }
                });
    }

    /**
     * חישוב שכר לחודש הנוכחי.
     */
    private void calculateMonthlySalary(String uid) {
        // חישוב היום הראשון בחודש
        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);

        // שליפת משמרות החודש
        db.collection("shifts")
                .whereArrayContains("assignedUserIds", uid)
                .whereGreaterThanOrEqualTo("startTime", startOfMonth.getTimeInMillis())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalHours = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Shift shift = doc.toObject(Shift.class);
                        if (shift != null) {
                            // חישוב משך זמן בשעות
                            long diff = shift.getEndTime() - shift.getStartTime();
                            double hours = (double) diff / (1000 * 60 * 60);
                            totalHours += hours;
                        }
                    }

                    // חישוב סופי
                    double salary = totalHours * hourlyRate;
                    binding.tvMonthlySalary.setText(String.format("₪%.2f", salary));
                });
    }

    private void setupButtons() {
        // כפתור רענון
        binding.btnRefreshData.setOnClickListener(v -> {
            Toast.makeText(this, "מרענן נתונים...", Toast.LENGTH_SHORT).show();
            refreshAllData();
        });

        // כפתור מעבר ליומן
        binding.btnViewSchedule.setOnClickListener(v -> {
            startActivity(new Intent(EmployeeActivity.this, EmployeeScheduleActivity.class));
        });

        // כפתור מעבר לדוח שכר
        binding.btnMySalary.setOnClickListener(v -> {
            startActivity(new Intent(EmployeeActivity.this, SalaryActivity.class));
        });

        // כפתור התנתקות
        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(EmployeeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}