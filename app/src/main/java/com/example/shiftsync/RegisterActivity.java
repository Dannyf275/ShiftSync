package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityRegisterBinding;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך ההרשמה (Register Screen).
 * מאפשר למשתמש חדש ליצור חשבון במערכת.
 * המשתמש מזין פרטים אישיים, בוחר תפקיד (עובד/מנהל), ומגדיר סיסמה.
 * המערכת מבצעת אימות נתונים לפני השליחה לשרת.
 */
public class RegisterActivity extends AppCompatActivity {

    // משתנה לגישה לרכיבי העיצוב (ViewBinding)
    private ActivityRegisterBinding binding;

    // רכיב האימות (ליצירת משתמש חדש)
    private FirebaseAuth mAuth;

    // רכיב מסד הנתונים (לשמירת פרטי המשתמש המורחבים)
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול ה-Binding
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // אתחול מופעי Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- לוגיקת שדה קוד מנהל ---
        // אנו מאזינים לשינויים בכפתורי הבחירה (Radio Buttons).
        // אם המשתמש בוחר "מנהל", אנו חושפים שדה להזנת קוד סודי.
        // אם הוא בוחר "עובד", השדה מוסתר.
        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbManager) {
                binding.etManagerCode.setVisibility(View.VISIBLE);
            } else {
                binding.etManagerCode.setVisibility(View.GONE);
            }
        });

        // הגדרת כפתור ההרשמה - מפעיל את הפונקציה registerUser
        binding.btnRegister.setOnClickListener(v -> registerUser());

        // כפתור חזרה למסך התחברות (למקרה שלחצו בטעות על הרשמה)
        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }

    /**
     * הפונקציה הראשית שמנהלת את תהליך ההרשמה.
     * היא מבצעת ולידציה (בדיקת תקינות) לכל השדות, ורק אם הכל תקין, פונה לשרת.
     */
    private void registerUser() {
        // 1. שליפת הנתונים מהשדות וניקוי רווחים (trim)
        String name = binding.etFullName.getText().toString().trim();
        String idNumber = binding.etIdNumber.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // 2. בדיקת התפקיד שנבחר
        boolean isManager = binding.rbManager.isChecked();
        // הגדרת המחרוזת לחיפוש ב-DB לפי הקבועים במחלקת User
        String role = isManager ? User.ROLE_MANAGER : User.ROLE_EMPLOYEE;

        // --- בדיקות תקינות (Validations) ---
        // בדיקות אלו מתבצעות מקומית במכשיר לפני הפנייה לשרת (חוסך זמן ומשאבים)

        if (TextUtils.isEmpty(name)) {
            binding.etFullName.setError("נא להזין שם מלא");
            return;
        }

        // בדיקה שתעודת הזהות מכילה בדיוק 9 ספרות
        if (TextUtils.isEmpty(idNumber) || idNumber.length() != 9) {
            binding.etIdNumber.setError("נא להזין ת.ז תקינה (9 ספרות)");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("נא להזין אימייל");
            return;
        }

        // סיסמה קצרה מדי לא מאובטחת וגם נדחית ע"י Firebase (מינימום 6)
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etPassword.setError("סיסמה חייבת להכיל 6 תווים לפחות");
            return;
        }

        // וידוא שהמשתמש לא טעה בהקלדת הסיסמה
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("הסיסמאות אינן תואמות");
            return;
        }

        // אימות קוד מנהל (רק אם נבחר תפקיד מנהל)
        // זהו מנגנון אבטחה בסיסי למניעת הרשמת סתם אנשים כמנהלים.
        if (isManager) {
            String code = binding.etManagerCode.getText().toString().trim();
            if (!code.equals("123456")) { // קוד קבוע שהוגדר מראש בפרויקט
                binding.etManagerCode.setError("קוד מנהל שגוי");
                return;
            }
        }

        // --- תהליך ההרשמה מול Firebase ---

        // הצגת טוען (Progress Bar) והקפאת הכפתור למניעת לחיצות כפולות
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        // שלב א': יצירת המשתמש ב-Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // ההרשמה הראשונית הצליחה! קיבלנו משתמש עם UID.
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // כעת עוברים לשלב ב': שמירת הפרטים הנוספים במסד הנתונים
                        saveUserToFirestore(firebaseUser.getUid(), name, idNumber, email, role);
                    }
                })
                .addOnFailureListener(e -> {
                    // כישלון בהרשמה (למשל: אימייל כבר קיים במערכת)
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "שגיאה בהרשמה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * פונקציה לשמירת פרטי המשתמש המלאים במסד הנתונים Firestore.
     * פונקציה זו נקראת רק לאחר ש-Authentication הצליח.
     */
    private void saveUserToFirestore(String uid, String name, String idNum, String email, String role) {
        // הגדרת שכר התחלתי כברירת מחדל (ניתן לעריכה אח"כ ע"י מנהל)
        double initialRate = 30.0;

        // יצירת אובייקט User מסודר (POJO)
        // סדר הפרמטרים בבנאי חייב להתאים למה שהגדרנו ב-User.java:
        // (UID, FullName, ID Number, Email, Role, Hourly Rate)
        User newUser = new User(uid, name, idNum, email, role, initialRate);

        // שמירה באוסף "users".
        // אנו משתמשים ב-UID כשם המסמך (.document(uid)) כדי שיהיה קל למצוא אותו בעתיד.
        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    // הכל עבר בהצלחה!
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "ההרשמה בוצעה בהצלחה!", Toast.LENGTH_SHORT).show();

                    // מעבר למסך ההתחברות (כדי שהמשתמש יתחבר עם הפרטים החדשים)
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);

                    // מחיקת היסטוריית המסכים - כדי שלחיצה על Back לא תחזיר להרשמה
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // מקרה נדיר: נוצר Auth אבל נכשל ה-Firestore.
                    // בפרויקט אמיתי אולי נרצה למחוק את ה-Auth במקרה כזה, אבל כאן נסתפק בהודעה.
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(this, "שגיאה בשמירת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}