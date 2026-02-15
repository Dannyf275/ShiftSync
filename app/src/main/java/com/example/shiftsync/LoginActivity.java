package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityLoginBinding;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

//מסך ההתחברות
public class LoginActivity extends AppCompatActivity {

    // משתנה לגישה לרכיבי העיצוב
    private ActivityLoginBinding binding;

    // רכיב האימות של פיירבייס אחראי על בדיקת אימייל וסיסמה
    private FirebaseAuth mAuth;

    // רכיב מסד הנתונים של פיירבייס אחראי על שליפת נתוני המשתמש
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  ניפוח קובץ ה-XML והפיכתו לאובייקטים של Java
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //  אתחול המופעים של פיירבייס
        // מקבלים את המופע הקיים של האפליקציה כדי שנוכל להשתמש בו
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //  הגדרת מאזין לכפתור ההתחברות
        // ברגע שהמשתמש לוחץ על התחבר הפונקציה loginUser תופעל
        binding.btnLogin.setOnClickListener(v -> loginUser());

        //  הגדרת מאזין לטקסט הרשמה
        // מעביר את המשתמש למסך ההרשמה
        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    //תהליך ההתחברות
    private void loginUser() {

        String email = binding.etLoginEmail.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא להזין אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        //  ביצוע ההתחברות מול פיירבייס
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // שליחת מזהה משתמש לבדיקת תפקיד
                    checkUserRole(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    // האימות נכשל
                    Toast.makeText(LoginActivity.this, "שגיאת התחברות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //בדיקת תפקיד המשתמש
    private void checkUserRole(String uid) {
        // פנייה לאוסף "users" ושליפת המסמך שמזהה שלו הוא ה-UID.
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // בדיקה האם המסמך באמת קיים
                    if (documentSnapshot.exists()) {

                        // המרת המסמך (JSON) לאובייקט Java מסוג User
                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            // אם ההמרה הצליחה, אנו בודקים את התפקיד ומנווטים בהתאם
                            navigateBasedOnRole(user.getRole());
                        }
                    } else {
                        // מקרה קצה -  המשתמש קיים ב-Auth (אימייל וסיסמה) אבל המידע שלו נמחק מהדאטה בייס
                        Toast.makeText(this, "משתמש לא נמצא במסד הנתונים", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // שגיאת רשת או הרשאות בגישה לפיירבייס
                    Toast.makeText(this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    //ניווט למסך האלוונטי לפי התפקיד
    private void navigateBasedOnRole(String role) {
        Intent intent;

        // שימוש בקבוע סטטי (ROLE_MANAGER) כדי למנוע שגיאות כתיב.
        if (User.ROLE_MANAGER.equals(role)) {
            // אם המשתמש הוא מנהל: פתח את מסך המנהל
            intent = new Intent(LoginActivity.this, ManagerActivity.class);
        } else {
            // אחרת (עובד): פתח את מסך העובד
            intent = new Intent(LoginActivity.this, EmployeeActivity.class);
        }


        // כשהמשתמש ייכנס למסך הראשי, לחיצה על "חזור" (Back) במכשיר תצא מהאפליקציה

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // סגירת ה-Activity הנוכחי (Login) כדי לשחרר זיכרון
    }
}