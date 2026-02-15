package com.example.shiftsync;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shiftsync.databinding.ActivityManagerBinding;
import com.example.shiftsync.models.Shift;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

//מסך מנהל
public class ManagerActivity extends AppCompatActivity {

    // גישה לרכיבי ה-XML באמצעות ViewBinding
    private ActivityManagerBinding binding;

    // אובייקטים לחיבור ל-Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // משתנה לטיפול בתוצאה של בחירת תמונה מהגלריה
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול ה-Binding
        binding = ActivityManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //  אתחול מנגנון בחירת התמונה (חייב להתבצע לפני השימוש)
        setupImagePicker();

        //  הגדרת לחיצה על תמונת הפרופיל -> פתיחת הגלריה
        binding.ivProfileImage.setOnClickListener(v -> openGallery());

        //  אתחול הגרפים וטעינת הנתונים
        setupPieChart();          // הגדרות עיצוב לגרף העוגה
        loadManagerDetails();     // הצגת שם המנהל והתמונה הנוכחית
        calculateMonthlyStats();  // חישוב נתונים לגרף ולסטטיסטיקה

        // 4. הגדרת כפתורי הניווט בתפריט הראשי
        setupButtons();
    }

    /**
     * הגדרת ה-Launcher שמקבל את התמונה שנבחרה מהגלריה.
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            // המרה מ-URI ל-Bitmap
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                            // שימוש במחלקת העזר ImageUtils להקטנת התמונה (קריטי למניעת קריסות זיכרון ו-Firebase)
                            Bitmap resized = ImageUtils.resizeBitmap(bitmap, 500);

                            // עדכון מיידי במסך (כדי שהמשתמש יראה שהצליח)
                            binding.ivProfileImage.setImageBitmap(resized);

                            // המרה ל-Base64 ושמירה בשרת ברקע
                            String base64 = ImageUtils.bitmapToString(resized);
                            saveImageToFirebase(base64);

                        } catch (IOException e) {
                            Toast.makeText(this, "שגיאה בטעינת תמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * פתיחת הגלריה של המכשיר לבחירת תמונה.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * עדכון שדה 'profileImage' במסמך המשתמש ב-Firestore.
     */
    private void saveImageToFirebase(String base64Image) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .update("profileImage", base64Image)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "התמונה נשמרה בהצלחה", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשמירה בשרת", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * טעינת פרטי המנהל (שם ותמונה קיימת) מהשרת בעת פתיחת המסך.
     */
    private void loadManagerDetails() {
        if(mAuth.getCurrentUser() == null) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if(doc.exists()) {
                        // עדכון כותרת "שלום, [שם]"
                        binding.tvWelcomeTitle.setText("שלום, " + doc.getString("fullName"));

                        // טעינת תמונה אם קיימת
                        String img = doc.getString("profileImage");
                        if(img != null && !img.isEmpty()) {
                            binding.ivProfileImage.setImageBitmap(ImageUtils.stringToBitmap(img));
                        }
                    }
                });
    }

    /**
     * הגדרות עיצוב ראשוניות לגרף העוגה (PieChart).
     * מסיר תוויות מיותרות ומגדיר את המקרא (Legend).
     */
    private void setupPieChart() {
        binding.pieChart.setDrawEntryLabels(false); // לא להציג טקסט על הפרוסות עצמן
        binding.pieChart.getDescription().setEnabled(false); // הסרת תיאור כללי
        binding.pieChart.setHoleColor(Color.WHITE); // צבע החור במרכז
        binding.pieChart.setCenterText("סטטוס\nחודשי"); // טקסט במרכז הגרף

        Legend l = binding.pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
    }

    /**
     * חישוב סטטיסטיקה חודשית:
     * 1. מגדיר טווח זמנים לחודש הנוכחי.
     * 2. שולף את כל המשמרות בטווח הזה.
     * 3. מחשב כמה משמרות מלאות וכמה עובדים חסרים.
     */
    private void calculateMonthlyStats() {
        // חישוב תחילת החודש וסוף החודש הנוכחי
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        // שליפת המשמרות
        db.collection("shifts")
                .whereGreaterThanOrEqualTo("startTime", start.getTimeInMillis())
                .whereLessThan("startTime", end.getTimeInMillis())
                .get()
                .addOnSuccessListener(snap -> {
                    int total = 0;   // סה"כ משמרות
                    int full = 0;    // כמה מהן מאוישות מלא
                    int missing = 0; // סה"כ עובדים שחסרים בכל המשמרות יחד

                    for(DocumentSnapshot d : snap.getDocuments()){
                        Shift s = d.toObject(Shift.class);
                        if(s != null){
                            total++;
                            // בדיקה כמה רשומים בפועל
                            int curr = (s.getAssignedUserIds() != null) ? s.getAssignedUserIds().size() : 0;

                            if(curr >= s.getRequiredWorkers()) {
                                full++; // המשמרת מלאה
                            } else {
                                missing += (s.getRequiredWorkers() - curr); // מוסיפים את ההפרש למונה החוסרים
                            }
                        }
                    }

                    // עדכון הטקסטים במסך
                    binding.tvStatTotalShifts.setText(String.valueOf(total));
                    binding.tvStatFullShifts.setText(String.valueOf(full));
                    binding.tvStatMissingWorkers.setText(String.valueOf(missing));

                    // עדכון הגרף הוויזואלי
                    updatePieChartData(total, full);
                });
    }

    /**
     * עדכון הנתונים בתוך גרף העוגה.
     * @param total - סה"כ משמרות.
     * @param full - סה"כ משמרות מלאות.
     */
    private void updatePieChartData(int total, int full) {
        if(total == 0) {
            binding.pieChart.clear();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();

        // הוספת נתונים רק אם הם גדולים מ-0
        if(full > 0) entries.add(new PieEntry(full, "מאוישות"));

        // החלק היחסי שאינו מלא
        if(total - full > 0) entries.add(new PieEntry(total - full, "חסר עובדים"));

        PieDataSet set = new PieDataSet(entries, "");
        // הגדרת צבעים: ירוק למלאות, אדום לחסרות
        set.setColors(new int[]{Color.parseColor("#4CAF50"), Color.parseColor("#E53935")});

        PieData data = new PieData(set);
        data.setValueTextSize(16f);
        data.setValueTextColor(Color.WHITE); // צבע המספרים על הגרף

        // פורמט למספרים שלמים (ללא נקודה עשרונית)
        data.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return String.valueOf((int)v); }
        });

        binding.pieChart.setData(data);
        binding.pieChart.invalidate(); // רענון הציור
    }

    /**
     * הגדרת כפתורי התפריט הראשי והמעבר למסכים השונים.
     */
    private void setupButtons() {
        // כפתור רענון נתונים ידני
        binding.btnRefreshData.setOnClickListener(v -> {
            loadManagerDetails();
            calculateMonthlyStats();
        });

        // מעבר לניהול עובדים
        binding.btnManageEmployees.setOnClickListener(v -> startActivity(new Intent(this, EmployeesListActivity.class)));

        // מעבר ליומן משמרות
        binding.btnShiftSchedule.setOnClickListener(v -> startActivity(new Intent(this, ManagerScheduleActivity.class)));

        // מעבר לבקשות לאישור
        binding.btnManageRequests.setOnClickListener(v -> startActivity(new Intent(this, ShiftRequestsActivity.class)));

        // מעבר ללוח הודעות (כפתור שהוספנו לאחרונה)
        binding.btnAnnouncements.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, ManageAnnouncementsActivity.class);
            startActivity(intent);
        });

        // כפתור התנתקות
        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // ניתוק מ-Firebase
            // חזרה למסך הכניסה ומחיקת היסטוריה
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }
}