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
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EmployeeActivity extends AppCompatActivity {

    private ActivityEmployeeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private double hourlyRate = 0.0; // נשמור את התעריף לחישובים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            // 1. טעינת פרטי משתמש (שם + תעריף)
            loadEmployeeDetails(uid);

            // 2. טעינת המשמרת הקרובה
            loadNextShift(uid);

            // 3. חישוב שכר לחודש הנוכחי
            calculateMonthlySalary(uid);
        }

        setupButtons();
    }

    private void loadEmployeeDetails(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.tvWelcomeTitle.setText("שלום, " + user.getFullName());
                            hourlyRate = user.getHourlyRate(); // שמירת התעריף

                            // אם השכר נטען אחרי חישוב המשמרות, נחשב שוב
                            calculateMonthlySalary(uid);
                        }
                    }
                });
    }

    private void loadNextShift(String uid) {
        long now = System.currentTimeMillis();

        // שאילתה: כל המשמרות העתידיות, ממוינות לפי זמן, שהמשתמש *מאושר* בהן
        db.collection("shifts")
                .whereGreaterThan("startTime", now)
                .whereArrayContains("assignedUserIds", uid) // רק מה שאושר!
                .orderBy("startTime", Query.Direction.ASCENDING)
                .limit(1) // רק את הראשונה
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Shift nextShift = queryDocumentSnapshots.getDocuments().get(0).toObject(Shift.class);
                        if (nextShift != null) {
                            SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

                            binding.tvNextShiftDate.setText(dateFmt.format(nextShift.getStartTime()));
                            binding.tvNextShiftTime.setText(timeFmt.format(nextShift.getStartTime()) + " - " + timeFmt.format(nextShift.getEndTime()));
                        }
                    } else {
                        binding.tvNextShiftDate.setText("אין משמרות קרובות");
                        binding.tvNextShiftTime.setText("");
                    }
                })
                .addOnFailureListener(e -> binding.tvNextShiftDate.setText("שגיאה בטעינה"));
    }

    private void calculateMonthlySalary(String uid) {
        // חישוב טווח החודש הנוכחי
        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);

        // שליפת כל המשמרות של החודש שאני רשום אליהן (Assigned)
        db.collection("shifts")
                .whereArrayContains("assignedUserIds", uid)
                .whereGreaterThanOrEqualTo("startTime", startOfMonth.getTimeInMillis())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalHours = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Shift shift = doc.toObject(Shift.class);
                        if (shift != null) {
                            // חישוב שעות
                            long diff = shift.getEndTime() - shift.getStartTime();
                            double hours = (double) diff / (1000 * 60 * 60);
                            totalHours += hours;
                        }
                    }

                    // חישוב שכר סופי
                    double salary = totalHours * hourlyRate;
                    binding.tvMonthlySalary.setText(String.format("₪%.2f", salary));
                });
    }

    private void setupButtons() {
        binding.btnViewSchedule.setOnClickListener(v -> {
            startActivity(new Intent(EmployeeActivity.this, EmployeeScheduleActivity.class));
        });

        binding.btnMySalary.setOnClickListener(v -> {
            startActivity(new Intent(EmployeeActivity.this, SalaryActivity.class));
        });

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(EmployeeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}