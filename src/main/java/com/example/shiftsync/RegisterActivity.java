package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityRegisterBinding;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך ההרשמה (Registration Screen).
 * מסך זה מטפל ביצירת משתמשים חדשים במערכת.
 * התהליך כולל:
 * 1. בדיקות תקינות קלט (שדות חובה, ת"ז תקינה, אימייל וכו').
 * 2. יצירת חשבון מאובטח ב-Firebase Authentication.
 * 3. יצירת מסמך פרופיל ב-Firebase Firestore עם הפרטים המלאים.
 */
public class RegisterActivity extends AppCompatActivity {

    // גישה לרכיבי העיצוב
    private ActivityRegisterBinding binding;

    // רכיבי Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול ViewBinding
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. לוגיקה להצגת/הסתרת שדה "קוד מנהל"
        // מאזינים לשינוי בבחירת כפתור הרדיו (RadioGroup)
        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbManager) {
                // אם נבחר "מנהל" -> הצג את שדה הסיסמה
                binding.etManagerCode.setVisibility(View.VISIBLE);
            } else {
                // אחרת -> הסתר
                binding.etManagerCode.setVisibility(View.GONE);
            }
        });

        // 4. כפתור הרשמה
        binding.btnRegister.setOnClickListener(v -> registerUser());

        // 5. כפתור "יש לך חשבון? התחבר"
        binding.tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish(); // סגירת ההרשמה כדי לחסוך זיכרון
        });
    }

    /**
     * הפונקציה הראשית שמבצעת את תהליך ההרשמה.
     */
    private void registerUser() {
        // שליפת הנתונים מהשדות וניקוי רווחים
        String name = binding.etFullName.getText().toString().trim();
        String idNumber = binding.etIdNumber.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // --- שלב א': בדיקות תקינות (Validations) ---

        if (TextUtils.isEmpty(name)) {
            binding.etFullName.setError("נא להזין שם מלא");
            return;
        }

        // בדיקת אורך ת"ז (חייב להיות 9 ספרות בישראל)
        if (TextUtils.isEmpty(idNumber) || idNumber.length() != 9) {
            binding.etIdNumber.setError("תעודת זהות חייבת להכיל 9 ספרות");
            return;
        }

        // בדיקת פורמט אימייל
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("אימייל לא תקין");
            return;
        }

        // בדיקת חוזק סיסמה (מינימום 6 תווים - דרישה של Firebase)
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etPassword.setError("סיסמה קצרה מדי");
            return;
        }

        // בדיקת התאמת סיסמאות
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("הסיסמאות אינן תואמות");
            return;
        }

        // קביעת התפקיד ובדיקת הרשאות מנהל
        String role = User.ROLE_EMPLOYEE; // ברירת מחדל
        if (binding.rbManager.isChecked()) {
            role = User.ROLE_MANAGER;
            String code = binding.etManagerCode.getText().toString().trim();
            // אימות קוד מנהל (Hardcoded כרגע)
            if (!"123456".equals(code)) {
                binding.etManagerCode.setError("קוד מנהל שגוי");
                return;
            }
        }

        // --- שלב ב': ביצוע ההרשמה ---

        // הצגת אינדיקטור טעינה ונטרול הכפתור למניעת לחיצות כפולות
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        String finalRole = role; // משתנה סופי לשימוש בתוך ה-Listener

        // 1. יצירת המשתמש ב-Authentication (אימייל + סיסמה)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d("REGISTER", "User created in Auth successfully");
                    // אם הצליח -> עוברים לשמירת הפרטים ב-Firestore
                    saveUserToFirestore(authResult.getUser().getUid(), name, email, finalRole, idNumber);
                })
                .addOnFailureListener(e -> {
                    // כישלון ב-Auth (למשל: אימייל כבר קיים)
                    Log.e("REGISTER", "Auth failed: " + e.getMessage());
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "שגיאה בהרשמה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * פונקציה לשמירת פרטי המשתמש המלאים ב-Firestore.
     */
    private void saveUserToFirestore(String uid, String name, String email, String role, String idNumber) {
        double initialRate = 0.0; // שכר התחלתי (המנהל יעדכן בהמשך)

        // יצירת אובייקט User (DTO)
        User newUser = new User(uid, name, email, role, initialRate, idNumber);

        // שמירת המסמך באוסף "users" תחת ה-UID של המשתמש
        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("REGISTER", "User data saved to Firestore. Moving to Login.");

                    // הצלחה מלאה!
                    Toast.makeText(RegisterActivity.this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();

                    // ניתוק המשתמש:
                    // Firebase מחבר אוטומטית אחרי הרשמה, אבל אנחנו רוצים
                    // שהמשתמש יבצע כניסה מסודרת דרך מסך הלוגין.
                    mAuth.signOut();

                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    // מקרה נדיר: נוצר ב-Auth אבל נכשל ב-Firestore
                    Log.e("REGISTER", "Firestore save failed: " + e.getMessage());
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "נרשמת, אך שמירת הפרטים נכשלה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * פונקציית עזר למעבר למסך ההתחברות.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        // מחיקת ההיסטוריה כדי שלחיצה על Back לא תחזיר להרשמה
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}