package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityLoginBinding;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך ההתחברות (Login Screen).
 * זהו שער הכניסה לאפליקציה. הוא מבצע תהליך אימות דו-שלבי:
 * 1. בדיקת שם משתמש וסיסמה מול Firebase Authentication.
 * 2. בדיקת התפקיד (מנהל/עובד) מול Firebase Firestore כדי לדעת לאן לנתב.
 */
public class LoginActivity extends AppCompatActivity {

    // משתנה לגישה לרכיבי העיצוב (ViewBinding)
    private ActivityLoginBinding binding;

    // רכיב האימות (בודק אימייל וסיסמה)
    private FirebaseAuth mAuth;

    // רכיב מסד הנתונים (בודק תפקיד ופרטים אישיים)
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול ה-ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול מופעי Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. בדיקה אם המשתמש כבר מחובר (אופציונלי - לשיפור חוויה)
        // אם היינו רוצים, היינו יכולים להוסיף כאן בדיקה של mAuth.getCurrentUser()
        // ולדלג ישר למסך הראשי ללא צורך בהתחברות מחדש.

        // 4. הגדרת כפתור התחברות
        binding.btnLogin.setOnClickListener(v -> loginUser());

        // 5. הגדרת כפתור מעבר להרשמה (למי שאין עדיין חשבון)
        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    /**
     * פונקציה שמבצעת את תהליך ההתחברות הראשוני.
     */
    private void loginUser() {
        // שליפת הטקסט מהשדות וניקוי רווחים מיותרים
        String email = binding.etLoginEmail.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString().trim();

        // בדיקת תקינות בסיסית (שהשדות לא ריקים)
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא להזין אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        // שלב 1: אימות מול שרת האימות (Authentication)
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // האימות הצליח! כעת המערכת יודעת שהסיסמה נכונה.
                    // אבל... אנחנו עדיין לא יודעים אם זה מנהל או עובד.
                    // לכן שולחים את ה-UID (המזהה) לבדיקה במסד הנתונים.
                    checkUserRole(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    // האימות נכשל (סיסמה שגויה / משתמש לא קיים)
                    Toast.makeText(LoginActivity.this, "שגיאת התחברות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * פונקציה שבודקת מה התפקיד של המשתמש שהתחבר כרגע.
     * @param uid המזהה הייחודי של המשתמש.
     */
    private void checkUserRole(String uid) {
        // ניגשים לאוסף "users" ושולפים את המסמך עם ה-UID הזה
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // המסמך קיים -> ממירים אותו לאובייקט User
                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            // מנווטים למסך המתאים לפי התפקיד השמור במסמך
                            navigateBasedOnRole(user.getRole());
                        }
                    } else {
                        // מקרה קצה: המשתמש קיים ב-Auth אבל נמחק מ-Firestore
                        Toast.makeText(this, "משתמש לא נמצא במסד הנתונים", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * פונקציית הניווט הסופית.
     * @param role התפקיד (manager/employee).
     */
    private void navigateBasedOnRole(String role) {
        Intent intent;

        // בדיקה: האם התפקיד הוא מנהל?
        if (User.ROLE_MANAGER.equals(role)) {
            intent = new Intent(LoginActivity.this, ManagerActivity.class);
        } else {
            // אם לא מנהל -> ברירת מחדל היא עובד
            intent = new Intent(LoginActivity.this, EmployeeActivity.class);
        }

        // ניקוי היסטוריית המסכים (Flags):
        // FLAG_ACTIVITY_NEW_TASK - מתחיל משימה חדשה.
        // FLAG_ACTIVITY_CLEAR_TASK - מוחק את כל המסכים שהיו לפני (כולל Login).
        // זה מונע מהמשתמש ללחוץ "Back" ולחזור למסך ההתחברות כשהוא כבר בפנים.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // סגירה פיזית של ה-Activity הנוכחי
    }
}