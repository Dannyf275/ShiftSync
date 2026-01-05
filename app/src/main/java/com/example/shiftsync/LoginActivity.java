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
 * זהו המסך הראשון שהמשתמש רואה (אלא אם הוא כבר מחובר).
 * תפקידו לאמת את זהות המשתמש ולנתב אותו למסך המתאים לפי התפקיד שלו.
 */
public class LoginActivity extends AppCompatActivity {

    // משתנה לגישה נוחה לרכיבי העיצוב (ViewBinding).
    // במקום להשתמש ב-findViewById כל פעם, אנחנו ניגשים דרך ה-binding.
    private ActivityLoginBinding binding;

    // רכיב האימות של Firebase. אחראי על בדיקת אימייל וסיסמה.
    private FirebaseAuth mAuth;

    // רכיב מסד הנתונים של Firebase. אחראי על שליפת נתוני המשתמש (כמו תפקיד).
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. "ניפוח" (Inflating) קובץ ה-XML והפיכתו לאובייקטים של Java.
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול המופעים של Firebase (Singleton).
        // מקבלים את המופע הקיים של האפליקציה כדי שנוכל להשתמש בו.
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. הגדרת מאזין (Listener) לכפתור ההתחברות.
        // ברגע שהמשתמש לוחץ על "התחבר", הפונקציה loginUser תופעל.
        binding.btnLogin.setOnClickListener(v -> loginUser());

        // 4. הגדרת מאזין לטקסט "אין לך חשבון? הירשם כאן".
        // מעביר את המשתמש למסך ההרשמה.
        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * פונקציה המבצעת את תהליך ההתחברות הלוגי.
     */
    private void loginUser() {
        // שלב א': שליפת המידע משדות הטקסט.
        // הפונקציה trim() מוחקת רווחים מיותרים בהתחלה ובסוף (למשל אם המקלדת הוסיפה רווח אוטומטי).
        String email = binding.etLoginEmail.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString().trim();

        // שלב ב': בדיקת תקינות קלט (Validation).
        // אם אחד השדות ריק, אין טעם לפנות לשרת.
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא להזין אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return; // עצירת הפונקציה כאן
        }

        // שלב ג': ביצוע ההתחברות מול Firebase Authentication.
        // זוהי פעולה אסינכרונית (לוקחת זמן), לכן יש לה callbacks של הצלחה וכישלון.
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // האימות הצליח! הסיסמה נכונה והמשתמש קיים.
                    // כעת עלינו לברר: האם זה מנהל או עובד?
                    // המידע הזה לא נמצא ב-Auth, אלא במסד הנתונים (Firestore).
                    // אנו שולחים את ה-UID (המזהה הייחודי) לפונקציית בדיקת התפקיד.
                    checkUserRole(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    // האימות נכשל (למשל: סיסמה שגויה, משתמש לא קיים, אין אינטרנט).
                    Toast.makeText(LoginActivity.this, "שגיאת התחברות: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * פונקציה שבודקת במסד הנתונים מהו התפקיד (Role) של המשתמש.
     * @param uid - המזהה הייחודי שקיבלנו מה-Authentication.
     */
    private void checkUserRole(String uid) {
        // פנייה לאוסף "users" ושליפת המסמך שמזהה שלו הוא ה-UID.
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // בדיקה האם המסמך באמת קיים ב-Firestore.
                    if (documentSnapshot.exists()) {

                        // המרת המסמך (JSON) לאובייקט Java מסוג User.
                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            // אם ההמרה הצליחה, אנו בודקים את התפקיד ומנווטים בהתאם.
                            navigateBasedOnRole(user.getRole());
                        }
                    } else {
                        // מקרה נדיר: המשתמש קיים ב-Auth (אימייל וסיסמה) אבל המידע שלו נמחק מהדאטה בייס.
                        Toast.makeText(this, "משתמש לא נמצא במסד הנתונים", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // שגיאת רשת או הרשאות בגישה ל-Firestore.
                    Toast.makeText(this, "שגיאה בשליפת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * פונקציית הניווט הסופית. פותחת את המסך המתאים לפי התפקיד.
     * @param role - מחרוזת התפקיד ("manager" או "employee").
     */
    private void navigateBasedOnRole(String role) {
        Intent intent;

        // שימוש בקבוע סטטי (ROLE_MANAGER) כדי למנוע שגיאות כתיב.
        if (User.ROLE_MANAGER.equals(role)) {
            // אם המשתמש הוא מנהל -> פתח את מסך המנהל
            intent = new Intent(LoginActivity.this, ManagerActivity.class);
        } else {
            // אחרת (עובד) -> פתח את מסך העובד
            intent = new Intent(LoginActivity.this, EmployeeActivity.class);
        }

        // הגדרת דגלים (Flags) ל-Intent:
        // 1. FLAG_ACTIVITY_NEW_TASK - פותח משימה חדשה.
        // 2. FLAG_ACTIVITY_CLEAR_TASK - מנקה את כל ההיסטוריה של המסכים הקודמים.
        // המשמעות: כשהמשתמש ייכנס למסך הראשי, לחיצה על "חזור" (Back) במכשיר
        // לא תחזיר אותו למסך ההתחברות, אלא תצא מהאפליקציה. זה קריטי לאבטחה ולחוויית משתמש.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // סגירת ה-Activity הנוכחי (Login) כדי לשחרר זיכרון.
    }
}