package com.example.shiftsync;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityEmployeeBinding;
import com.example.shiftsync.models.Announcement;
import com.example.shiftsync.models.Shift;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * מסך הבית של העובד (Employee Dashboard).
 * מסך זה מרכז את כל המידע הרלוונטי לעובד:
 * 1. הצגת שם ותמונה.
 * 2. הצגת המשמרת הקרובה ביותר.
 * 3. חישוב שכר חודשי משוער.
 * 4. הצגת הודעות מערכת אחרונות מהמנהל.
 * 5. ניווט למסכים נוספים (לוח משמרות, דוח שכר).
 */
public class EmployeeActivity extends AppCompatActivity {

    // קישור לרכיבי התצוגה (XML) באמצעות ViewBinding
    private ActivityEmployeeBinding binding;

    // אובייקטים לניהול חיבור ונתונים ב-Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // משתנה לניהול תהליך בחירת התמונה מהגלריה
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול ה-Binding (מחליף את findViewById)
        binding = ActivityEmployeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // אתחול מופעי Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // אתחול המנגנון לבחירת תמונה (חובה לבצע לפני השימוש)
        setupImagePicker();

        // --- טעינת נתונים ראשונית ---
        loadEmployeeData();        // שם ותמונה
        loadNextShift();           // המשמרת הבאה
        calculateMonthlySalary();  // שכר משוער
        loadLatestAnnouncement();  // הודעה אחרונה

        // --- הגדרת כפתורים (Listeners) ---

        // כפתור רענון ידני (למקרה שהנתונים לא התעדכנו)
        binding.btnRefreshData.setOnClickListener(v -> {
            Toast.makeText(this, "מרענן נתונים...", Toast.LENGTH_SHORT).show();
            loadEmployeeData();
            loadNextShift();
            calculateMonthlySalary();
            loadLatestAnnouncement();
        });

        // לחיצה על תמונת הפרופיל -> פתיחת הגלריה
        binding.ivProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // מעבר למסך "לוח משמרות" (הרשמה למשמרות)
        binding.btnViewSchedule.setOnClickListener(v -> startActivity(new Intent(this, EmployeeScheduleActivity.class)));

        // מעבר למסך "דוח שכר" (PDF ופירוט)
        binding.btnMySalary.setOnClickListener(v -> startActivity(new Intent(this, SalaryActivity.class)));

        // התנתקות מהמערכת
        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // ניתוק מ-Firebase Auth
            // מעבר למסך התחברות ומחיקת היסטוריית המסכים (כדי שלא יוכל לחזור אחורה)
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    /**
     * פונקציה לטעינת ההודעה האחרונה שפורסמה ע"י המנהל.
     * משתמשת ב-SnapshotListener כדי להאזין לשינויים בזמן אמת.
     */
    private void loadLatestAnnouncement() {
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING) // מיון לפי זמן (מהחדש לישן)
                .limit(1) // אנחנו רוצים רק את ההודעה הכי חדשה
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // אם יש שגיאה, נסתיר את הכרטיס
                        binding.cardAnnouncement.setVisibility(View.GONE);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        // אם נמצאה הודעה, נחלץ אותה
                        DocumentSnapshot doc = value.getDocuments().get(0);
                        Announcement announcement = doc.toObject(Announcement.class);

                        if (announcement != null) {
                            // 1. חשיפת הכרטיס למשתמש
                            binding.cardAnnouncement.setVisibility(View.VISIBLE);

                            // 2. הזנת התוכן (כותרת, תוכן, תאריך)
                            binding.tvAnnTitle.setText(announcement.getTitle());
                            binding.tvAnnContent.setText(announcement.getContent());

                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                            binding.tvAnnDate.setText(sdf.format(announcement.getTimestamp()));
                        }
                    } else {
                        // אם אין הודעות בכלל במסד הנתונים -> נסתיר את הכרטיס
                        binding.cardAnnouncement.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * פונקציה למציאת המשמרת הקרובה ביותר של העובד.
     * הלוגיקה: שולפים את כל המשמרות העתידיות, ומסננים בצד הלקוח (באפליקציה)
     * את המשמרת הראשונה שבה העובד מופיע ברשימת ה-assignedUserIds.
     */
    private void loadNextShift() {
        long now = System.currentTimeMillis();
        String uid = mAuth.getCurrentUser().getUid();

        // שאילתה: כל המשמרות שזמן ההתחלה שלהן הוא בעתיד, ממוינות לפי זמן עולה.
        db.collection("shifts")
                .whereGreaterThan("startTime", now)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Shift nextShift = null;

                    // לולאת סינון: מחפשים את המשמרת הראשונה שהעובד משובץ אליה
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Shift shift = doc.toObject(Shift.class);
                        if (shift != null && shift.getAssignedUserIds() != null && shift.getAssignedUserIds().contains(uid)) {
                            nextShift = shift;
                            break; // מצאנו את הקרובה ביותר, מפסיקים לחפש
                        }
                    }

                    // עדכון התצוגה במסך
                    if (nextShift != null) {
                        SimpleDateFormat dateF = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        SimpleDateFormat timeF = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        binding.tvNextShiftDate.setText(dateF.format(nextShift.getStartTime()));
                        binding.tvNextShiftTime.setText(timeF.format(nextShift.getStartTime()) + " - " + timeF.format(nextShift.getEndTime()));
                    } else {
                        binding.tvNextShiftDate.setText("אין משמרות קרובות");
                        binding.tvNextShiftTime.setText("--:--");
                    }
                })
                .addOnFailureListener(e -> {
                    // טיפול בשגיאות
                    binding.tvNextShiftDate.setText("שגיאה בטעינה");
                });
    }

    /**
     * הגדרת ה-Launcher לטיפול בתוצאה של בחירת תמונה מהגלריה.
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            // המרה מ-URI ל-Bitmap
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                            // שימוש במחלקת העזר ImageUtils להקטנת התמונה (קריטי לביצועים!)
                            Bitmap resized = ImageUtils.resizeBitmap(bitmap, 500);

                            // הצגה מיידית למשתמש
                            binding.ivProfileImage.setImageBitmap(resized);

                            // שמירה ב-Firebase
                            String base64Image = ImageUtils.bitmapToString(resized);
                            saveImageToFirebase(base64Image);

                        } catch (IOException e) {
                            Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * שמירת מחרוזת התמונה (Base64) במסמך המשתמש ב-Firestore.
     */
    private void saveImageToFirebase(String base64Image) {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .update("profileImage", base64Image)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "תמונת פרופיל עודכנה!", Toast.LENGTH_SHORT).show());
    }

    /**
     * טעינת פרטי העובד (שם ותמונה) להצגה בראש המסך (Header).
     */
    private void loadEmployeeData() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.tvWelcomeTitle.setText("שלום, " + user.getFullName());

                            // אם קיימת תמונה שמורה, נטען אותה
                            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                                Bitmap bmp = ImageUtils.stringToBitmap(user.getProfileImage());
                                if (bmp != null) binding.ivProfileImage.setImageBitmap(bmp);
                            }
                        }
                    }
                });
    }

    /**
     * חישוב שכר חודשי משוער.
     * שלב 1: שליפת השכר השעתי של העובד.
     * שלב 2: שליפת כל המשמרות בחודש הנוכחי שבהן העובד שובץ.
     * שלב 3: סיכום שעות * תעריף.
     */
    private void calculateMonthlySalary() {
        String uid = mAuth.getCurrentUser().getUid();

        // שליפת נתוני המשתמש לקבלת התעריף השעתי
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) return;

            Double rate = userDoc.getDouble("hourlyRate");
            if (rate == null) rate = 0.0;
            final double hourlyRate = rate;

            // הגדרת טווח הזמנים: מתחילת החודש ועד עכשיו (או סוף החודש)
            Calendar startOfMonth = Calendar.getInstance();
            startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
            startOfMonth.set(Calendar.HOUR_OF_DAY, 0);

            // שאילתה למשמרות החודש
            db.collection("shifts")
                    .whereArrayContains("assignedUserIds", uid)
                    .whereGreaterThanOrEqualTo("startTime", startOfMonth.getTimeInMillis())
                    .get()
                    .addOnSuccessListener(shiftsSnap -> {
                        double totalHours = 0;
                        for (DocumentSnapshot doc : shiftsSnap.getDocuments()) {
                            Shift s = doc.toObject(Shift.class);
                            if (s != null) {
                                // חישוב משך המשמרת בשעות
                                long duration = s.getEndTime() - s.getStartTime();
                                totalHours += (double) duration / (1000 * 60 * 60);
                            }
                        }
                        // עדכון התצוגה עם הסימן ש"ח
                        binding.tvMonthlySalary.setText(String.format(Locale.getDefault(), "₪%.2f", totalHours * hourlyRate));
                    });
        });
    }
}