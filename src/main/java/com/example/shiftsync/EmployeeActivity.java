package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityEmployeeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך הבית של העובד (Dashboard).
 * מכיל ברכה אישית ואפשרות ניווט ללוח המשמרות לביצוע שיבוץ.
 */
public class EmployeeActivity extends AppCompatActivity {

    // משתנה לגישה לרכיבי העיצוב (ViewBinding)
    private ActivityEmployeeBinding binding;

    // משתנים לחיבור ל-Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול ViewBinding (מחליף את findViewById)
        binding = ActivityEmployeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול מופעי Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. טעינת שם העובד מהענן
        loadEmployeeDetails();

        // 4. הגדרת הפעולות לכפתורים
        setupButtons();
    }

    /**
     * פונקציה לשליפת שם המשתמש מ-Firestore והצגתו בכותרת
     */
    private void loadEmployeeDetails() {
        // בדיקה בטיחותית שהמשתמש מחובר
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // שליפת המסמך מאוסף users לפי ה-UID
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // אם נמצא המסמך, נשלוף את השדה fullName
                        String name = documentSnapshot.getString("fullName");
                        if (name != null) {
                            binding.tvWelcomeTitle.setText("שלום, " + name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // במקרה של שגיאה נשאיר את טקסט ברירת המחדל
                    Toast.makeText(this, "לא ניתן לטעון פרטים", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * הגדרת המאזינים ללחיצות על הכפתורים במסך
     */
    private void setupButtons() {

        // כפתור מעבר ליומן המשמרות
        binding.btnViewSchedule.setOnClickListener(v -> {
            // מפנים למסך הייעודי לעובד (ולא למסך המנהל)
            Intent intent = new Intent(EmployeeActivity.this, EmployeeScheduleActivity.class);
            startActivity(intent);
        });

        // כפתור התנתקות
        binding.btnLogout.setOnClickListener(v -> {
            // 1. ביצוע LogOut מ-Firebase
            mAuth.signOut();

            // 2. חזרה למסך ההתחברות
            Intent intent = new Intent(EmployeeActivity.this, LoginActivity.class);

            // 3. ניקוי ההיסטוריה (כדי שלחיצה על Back לא תחזיר אותנו פנימה)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }
}