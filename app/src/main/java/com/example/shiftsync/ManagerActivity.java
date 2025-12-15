package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityManagerBinding;
import com.example.shiftsync.models.Shift;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

/**
 * מסך הבית של המנהל (Manager Dashboard).
 * כולל כפתור סנכרון לטעינה מחדש של הנתונים וכרטיס סטטיסטיקה חודשית.
 */
public class ManagerActivity extends AppCompatActivity {

    private ActivityManagerBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // טעינה ראשונית של שם וסטטיסטיקות
        loadManagerDetails();
        calculateMonthlyStats();

        setupButtons();
    }

    /**
     * פונקציה לחישוב נתונים סטטיסטיים לחודש הנוכחי.
     */
    private void calculateMonthlyStats() {
        // קביעת טווח הזמנים: מתחילת החודש הנוכחי
        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);

        // קביעת סוף החודש (תחילת החודש הבא)
        Calendar endOfMonth = (Calendar) startOfMonth.clone();
        endOfMonth.add(Calendar.MONTH, 1);

        // שאילתה לכל המשמרות שמתחילות החודש
        db.collection("shifts")
                .whereGreaterThanOrEqualTo("startTime", startOfMonth.getTimeInMillis())
                .whereLessThan("startTime", endOfMonth.getTimeInMillis())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalShifts = 0;
                    int fullShifts = 0;
                    int missingWorkersCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Shift shift = doc.toObject(Shift.class);
                        if (shift != null) {
                            totalShifts++;

                            // חישוב כמה עובדים רשומים כרגע
                            int currentAssigned = (shift.getAssignedUserIds() != null) ? shift.getAssignedUserIds().size() : 0;
                            int required = shift.getRequiredWorkers();

                            // בדיקה אם המשמרת מלאה
                            if (currentAssigned >= required) {
                                fullShifts++;
                            } else {
                                // אם לא מלאה, כמה עובדים חסרים?
                                missingWorkersCount += (required - currentAssigned);
                            }
                        }
                    }

                    // עדכון התצוגה
                    binding.tvStatTotalShifts.setText(String.valueOf(totalShifts));
                    binding.tvStatFullShifts.setText(String.valueOf(fullShifts));
                    binding.tvStatMissingWorkers.setText(String.valueOf(missingWorkersCount));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בחישוב סטטיסטיקה", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadManagerDetails() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        if (name != null) {
                            binding.tvWelcomeTitle.setText("שלום, " + name);
                        }
                    }
                });
    }

    private void setupButtons() {
        // --- כפתור רענון ---
        binding.btnRefreshData.setOnClickListener(v -> {
            Toast.makeText(this, "מרענן נתונים...", Toast.LENGTH_SHORT).show();
            loadManagerDetails();
            calculateMonthlyStats(); // חישוב מחדש בלחיצה
        });

        binding.btnManageEmployees.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, EmployeesListActivity.class);
            startActivity(intent);
        });

        binding.btnShiftSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, ManagerScheduleActivity.class);
            startActivity(intent);
        });

        binding.btnManageRequests.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, ShiftRequestsActivity.class);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ManagerActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}