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

//מסך הפתיחה - טעינה של 3 שניות, בדיקת האם משתמש מחובר ומעבר למסך המתאים
public class MainActivity extends AppCompatActivity {

    // קבוע המגדיר את זמן ההשהייה
    // בזמן הזה המשתמש יראה את הלוגו
    private static final int SPLASH_DELAY = 3000;

    // אובייקטים לחיבור לפיירבייס
    private FirebaseAuth mAuth; // לבדיקת המשתמש המחובר
    private FirebaseFirestore db; // לבדיקת התפקיד

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת עיצוב המסך (הלוגו)
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserStatus();

        }, SPLASH_DELAY);
    }


    private void checkUserStatus() {
        // בדיקה האם יש משתמש שמחובר כרגע
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            // בדיקת תפקיד המשתמש המחובר
            checkRoleAndNavigate(currentUser.getUid());
        } else {
            // אם לא מחובר ניווט למסך התחברות
            navigateToLogin();
        }
    }

    //בדיקת תפקיד וניווט
    private void checkRoleAndNavigate(String uid) {
        // שליפת המסמך של המשתמש מאוסף המשתמשים
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // בדיקה האם המסמך קיים
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

    //מעבר למסך התחברות
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // ניקוי ההיסטוריה: מונע חזרה למסך הפתיחה בלחיצה על Back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // סגירת ה-Splash Screen
    }

    //מעבר למסך עובד/מנהל
    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(MainActivity.this, targetActivity);
        // ניקוי ההיסטוריה: מונע חזרה למסך הפתיחה בלחיצה על Back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // סגירת ה-Splash Screen
    }
}