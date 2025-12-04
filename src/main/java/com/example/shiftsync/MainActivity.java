package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // <-- חשוב: ייבוא של Handler
import android.os.Looper;  // <-- חשוב: ייבוא של Looper
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך הפתיחה (Splash Screen).
 * מציג את הלוגו למשך מספר שניות, בודק התחברות, ומעביר למסך הבא.
 */
public class MainActivity extends AppCompatActivity {

    // הגדרת זמן ההשהייה (במילי-שניות). 3000 = 3 שניות.
    private static final int SPLASH_DELAY = 3000;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. הפעלת טיימר להשהייה
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // הקוד הזה ירוץ רק אחרי שהזמן (SPLASH_DELAY) יעבור
            checkUserStatus();

        }, SPLASH_DELAY);
    }

    /**
     * בדיקת סטטוס המשתמש (האם מחובר?) וניתוב בהתאם.
     */
    private void checkUserStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // --- משתמש מחובר ---
            // בודקים תפקיד ומעבירים למסך הראשי
            checkRoleAndNavigate(currentUser.getUid());
        } else {
            // --- לא מחובר ---
            // מעבר למסך התחברות
            navigateToLogin();
        }
    }

    private void checkRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            if (User.ROLE_MANAGER.equals(user.getRole())) {
                                navigateToActivity(ManagerActivity.class);
                            } else {
                                navigateToActivity(EmployeeActivity.class);
                            }
                        } else {
                            navigateToLogin();
                        }
                    } else {
                        Toast.makeText(this, "משתמש לא נמצא", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        navigateToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאת רשת: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(MainActivity.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}