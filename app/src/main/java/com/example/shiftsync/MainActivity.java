package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // משמש להרצת קוד עם השהייה
import android.os.Looper;  // משמש לגישה ל-Thread הראשי (UI Thread)
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך הפתיחה (Splash Screen).
 * מציג את הלוגו למשך מספר שניות, בודק את סטטוס ההתחברות, ומעביר למסך הבא.
 */
public class MainActivity extends AppCompatActivity {

    // קבוע המגדיר את זמן ההשהייה במילי-שניות (3000 = 3 שניות).
    // בזמן הזה המשתמש יראה את הלוגו (שהוגדר ב-activity_main.xml).
    private static final int SPLASH_DELAY = 3000;

    // אובייקטים לחיבור ל-Firebase
    private FirebaseAuth mAuth; // לבדיקת המשתמש המחובר
    private FirebaseFirestore db; // לבדיקת התפקיד (Role)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת עיצוב המסך (הלוגו)
        setContentView(R.layout.activity_main);

        // 1. אתחול מופעי Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. הפעלת מנגנון ההשהייה (Timer).
        // אנחנו משתמשים ב-Handler המקושר ל-MainLooper כדי לוודא
        // שהקוד ירוץ על ה-UI Thread אחרי שהזמן יעבור.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // הקוד בתוך הבלוק הזה ירוץ רק אחרי 3 שניות (SPLASH_DELAY).
            // כעת אנחנו בודקים לאן להעביר את המשתמש.
            checkUserStatus();

        }, SPLASH_DELAY);
    }

    /**
     * פונקציה הבודקת את סטטוס המשתמש (Auto Login).
     */
    private void checkUserStatus() {
        // בדיקה האם יש משתמש שמחובר כרגע (שמר session מפעם קודמת)
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // --- משתמש מחובר ---
            // אנחנו יודעים שהוא מחובר, אבל לא יודעים אם הוא מנהל או עובד.
            // לכן צריך לבדוק את התפקיד שלו ב-Firestore.
            checkRoleAndNavigate(currentUser.getUid());
        } else {
            // --- לא מחובר ---
            // המשתמש צריך להזין שם וסיסמה -> מעבירים למסך התחברות.
            navigateToLogin();
        }
    }

    /**
     * פונקציה שבודקת ב-Firestore מה התפקיד של המשתמש ומנווטת בהתאם.
     * @param uid - המזהה הייחודי של המשתמש.
     */
    private void checkRoleAndNavigate(String uid) {
        // שליפת המסמך של המשתמש מאוסף "users"
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // בדיקה האם המסמך קיים (למניעת קריסות אם המשתמש נמחק מהדאטה בייס)
                    if (documentSnapshot.exists()) {

                        // המרה לאובייקט User
                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            // בדיקת התפקיד
                            if (User.ROLE_MANAGER.equals(user.getRole())) {
                                // אם מנהל -> לך למסך מנהל
                                navigateToActivity(ManagerActivity.class);
                            } else {
                                // אחרת (עובד) -> לך למסך עובד
                                navigateToActivity(EmployeeActivity.class);
                            }
                        } else {
                            // אם היתה בעיה בהמרה, חזור להתחברות
                            navigateToLogin();
                        }
                    } else {
                        // מקרה קצה: המשתמש קיים ב-Auth אבל לא ב-Firestore.
                        // מנתקים אותו ומחזירים להתחברות.
                        Toast.makeText(this, "משתמש לא נמצא במערכת", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        navigateToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    // במקרה של שגיאת רשת (אין אינטרנט וכו'), לא ניתן לבדוק תפקיד.
                    // לכן נעביר למסך ההתחברות כדי שינסה שוב.
                    Toast.makeText(this, "שגיאת רשת: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
    }

    /**
     * פונקציית עזר למעבר למסך ההתחברות.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // ניקוי ההיסטוריה: מונע חזרה למסך הפתיחה בלחיצה על Back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // סגירת ה-Splash Screen
    }

    /**
     * פונקציית עזר גנרית למעבר לכל Activity אחר (מנהל או עובד).
     * @param targetActivity - המחלקה של המסך שאליו רוצים לעבור.
     */
    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(MainActivity.this, targetActivity);
        // ניקוי ההיסטוריה: מונע חזרה למסך הפתיחה בלחיצה על Back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // סגירת ה-Splash Screen
    }
}