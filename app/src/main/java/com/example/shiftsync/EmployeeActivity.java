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

//מסך עובד
public class EmployeeActivity extends AppCompatActivity {

    // קישור לרכיבי התצוגה
    private ActivityEmployeeBinding binding;

    // חיבור ושליפת נתונים מהפיירבייס - אובייקטים
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // בחירת תמונה מהגלריה
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // הפעלת ה-Binding
        binding = ActivityEmployeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // הפעלת מופעי פיירבייס
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // הפעלת מנגנון בחירת תמונה
        setupImagePicker();

        // טעינה ראשונה
        loadEmployeeData();        // שם ותמונה
        loadNextShift();           // המשמרת הבאה
        calculateMonthlySalary();  // שכר משוער
        loadLatestAnnouncement();  // הודעה אחרונה

        // מאזינים

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

        // מעבר למסך לוח משמרות
        binding.btnViewSchedule.setOnClickListener(v -> startActivity(new Intent(this, EmployeeScheduleActivity.class)));

        // מעבר למסך דוח שכר
        binding.btnMySalary.setOnClickListener(v -> startActivity(new Intent(this, SalaryActivity.class)));

        // התנתקות מהמערכת
        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // ניתוק מ-Firebase Auth
            // מעבר למסך התחברות ומחיקת היסטוריית המסכים (כדי שלא יוכל לחזור אחורה)
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    //טעינת הודעות מנהל
    private void loadLatestAnnouncement() {
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING) // מיון לפי זמן (מהחדש לישן)
                .limit(1) // אנחנו רוצים רק את ההודעה הכי חדשה
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        binding.cardAnnouncement.setVisibility(View.GONE);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        DocumentSnapshot doc = value.getDocuments().get(0);
                        Announcement announcement = doc.toObject(Announcement.class);

                        if (announcement != null) {
                            binding.cardAnnouncement.setVisibility(View.VISIBLE);

                            //  הזנת התוכן (כותרת, תוכן, תאריך)
                            binding.tvAnnTitle.setText(announcement.getTitle());
                            binding.tvAnnContent.setText(announcement.getContent());

                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                            binding.tvAnnDate.setText(sdf.format(announcement.getTimestamp()));
                        }
                    } else {
                        // אם אין הודעות בכלל במסד הנתונים נסתיר את הכרטיס
                        binding.cardAnnouncement.setVisibility(View.GONE);
                    }
                });
    }

    //מציאת המשמרת הבאה של העובד
    private void loadNextShift() {
        long now = System.currentTimeMillis();
        String uid = mAuth.getCurrentUser().getUid();

        // שאילתה: כל המשמרות שזמן ההתחלה שלהן הוא בעתיד, ממוינות לפי זמן עולה
        db.collection("shifts")
                .whereGreaterThan("startTime", now)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Shift nextShift = null;

                    // מחפשים את המשמרת הראשונה שהעובד משובץ אליה
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Shift shift = doc.toObject(Shift.class);
                        if (shift != null && shift.getAssignedUserIds() != null && shift.getAssignedUserIds().contains(uid)) {
                            nextShift = shift;
                            break;
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


      //הגדרת ה-Launcher לטיפול בתוצאה של בחירת תמונה מהגלריה

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            // המרה מ-URI ל-Bitmap
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                            // שימוש במחלקת העזר ImageUtils להקטנת התמונה
                            Bitmap resized = ImageUtils.resizeBitmap(bitmap, 500);

                            // הצגה מיידית למשתמש
                            binding.ivProfileImage.setImageBitmap(resized);

                            // שמירה בפיירבייס
                            String base64Image = ImageUtils.bitmapToString(resized);
                            saveImageToFirebase(base64Image);

                        } catch (IOException e) {
                            Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    //שמירת התמונה בפיירבייס
    private void saveImageToFirebase(String base64Image) {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .update("profileImage", base64Image)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "תמונת פרופיל עודכנה!", Toast.LENGTH_SHORT).show());
    }

    //טעינת פרטי העובד בראש המסך
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

    //חישוב שכר חודשי
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