package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityManagerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך הבית של המנהל (Manager Dashboard).
 * מסך זה מרכז את כל הפעולות שהמנהל יכול לבצע:
 * 1. מעבר לניהול עובדים (רשימה, עריכה, הוספה).
 * 2. מעבר ליומן משמרות (יצירה ושיבוץ).
 * 3. התנתקות מהמערכת.
 */
public class ManagerActivity extends AppCompatActivity {

    // גישה לרכיבי העיצוב (ViewBinding)
    private ActivityManagerBinding binding;

    // מופעי Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול ViewBinding
        binding = ActivityManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. טעינת שם המנהל (לכותרת "שלום X")
        loadManagerDetails();

        // 4. הגדרת הכפתורים
        setupButtons();
    }

    /**
     * פונקציה לשליפת פרטי המנהל הנוכחי מ-Firestore.
     * המטרה: להציג את שמו בכותרת המסך.
     */
    private void loadManagerDetails() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // בדיקת בטיחות: אם משום מה אין משתמש מחובר, לא ממשיכים
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // שליפת השם ועדכון הכותרת
                        String name = documentSnapshot.getString("fullName");
                        if (name != null) {
                            binding.tvWelcomeTitle.setText("שלום, " + name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // במקרה של שגיאה, נציג כותרת גנרית
                    binding.tvWelcomeTitle.setText("שלום, מנהל");
                });
    }

    /**
     * פונקציה שמגדירה את כל המאזינים (Listeners) לכפתורים במסך.
     */
    private void setupButtons() {
        // --- כפתור 1: מעבר לניהול עובדים ---
        binding.btnManageEmployees.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, EmployeesListActivity.class);
            startActivity(intent);
        });

        // --- כפתור 2: מעבר ליומן משמרות ---
        binding.btnShiftSchedule.setOnClickListener(v -> {
            // מעבר למסך הלוח שנה החכם שבנינו (ולא לטופס הישן)
            Intent intent = new Intent(ManagerActivity.this, ManagerScheduleActivity.class);
            startActivity(intent);
        });

        //כפתור 3: אישור בקשת משמרת
        binding.btnManageRequests.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, ShiftRequestsActivity.class);
            startActivity(intent);
        });

        // --- כפתור 4: התנתקות ---
        binding.btnLogout.setOnClickListener(v -> {
            // 1. ניתוק מ-Firebase Auth
            mAuth.signOut();

            // 2. חזרה למסך ההתחברות
            Intent intent = new Intent(ManagerActivity.this, LoginActivity.class);

            // 3. מחיקת היסטוריית הגלישה (Flags) כדי שלחיצה על Back לא תחזיר למסך המנהל
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish(); // סגירת ה-Activity הנוכחי
        });


    }
}