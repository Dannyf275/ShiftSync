package com.example.shiftsync;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shiftsync.databinding.ActivitySalaryBinding;
import com.example.shiftsync.models.Shift;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * מסך דוח שכר (Salary Report Activity).
 * מסך זה מבצע חישוב של השכר הצפוי לחודש הנוכחי ומאפשר ייצוא לדוח PDF.
 * הפעולות העיקריות:
 * 1. שליפת השכר השעתי של העובד.
 * 2. שליפת כל המשמרות שהעובד ביצע החודש.
 * 3. חישוב מתמטי (שעות * תעריף).
 * 4. יצירת קובץ PDF בצורה גרפית (ציור טקסט וקווים).
 */
public class SalaryActivity extends AppCompatActivity {

    private ActivitySalaryBinding binding;
    private FirebaseFirestore db;

    // משתנים לנתוני המשתמש והחישובים
    private String currentUserId;
    private double userHourlyRate = 0; // ברירת מחדל 0 עד שנטען מהשרת
    private String userFullName = "עובד";

    // רשימת המשמרות שתחושב
    private List<Shift> shiftsList;
    private SalaryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        shiftsList = new ArrayList<>();

        // הגדרת רשימת התצוגה (RecyclerView)
        binding.rvSalaryShifts.setLayoutManager(new LinearLayoutManager(this));

        // שלב 1: טעינת נתוני משתמש.
        // אנו חייבים לטעון קודם את המשתמש כדי לקבל את ה"תעריף השעתי" (Hourly Rate).
        // רק אחרי שנקבל אותו, נוכל לטעון את המשמרות ולחשב שכר.
        loadUserData();

        // כפתור חזרה
        binding.btnBack.setOnClickListener(v -> finish());

        // כפתור ייצוא ל-PDF -> מפעיל את הפונקציה המורכבת של יצירת המסמך
        binding.btnExportPdf.setOnClickListener(v -> generatePdfReport());
    }

    /**
     * שליפת נתוני המשתמש (שם ותעריף שעתי) מ-Firestore.
     */
    private void loadUserData() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // שמירת הנתונים במשתנים הגלובליים
                            userHourlyRate = user.getHourlyRate();
                            userFullName = user.getFullName();

                            // שלב 2: כעת שיש לנו תעריף, נטען את המשמרות
                            loadMonthlyShifts();
                        }
                    }
                });
    }

    /**
     * טעינת המשמרות של החודש הנוכחי וחישוב השכר הכולל.
     */
    private void loadMonthlyShifts() {
        // חישוב תאריך תחילת החודש (למשל: 1 לינואר 00:00)
        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);

        // ביצוע שאילתה מורכבת:
        // 1. האם ה-ID שלי נמצא ברשימת assignedUserIds? (האם אני משובץ?)
        // 2. האם תאריך המשמרת גדול או שווה לתחילת החודש?
        db.collection("shifts")
                .whereArrayContains("assignedUserIds", currentUserId)
                .whereGreaterThanOrEqualTo("startTime", startOfMonth.getTimeInMillis())
                .orderBy("startTime") // סידור כרונולוגי
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    shiftsList.clear();
                    double totalSalary = 0;

                    // מעבר על כל המשמרות שנמצאו
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Shift shift = doc.toObject(Shift.class);
                        if (shift != null) {
                            shiftsList.add(shift);

                            // חישוב שכר למשמרת בודדת:
                            // (זמן סיום - זמן התחלה) במילישניות -> המרה לשעות -> כפול תעריף
                            double hours = (double) (shift.getEndTime() - shift.getStartTime()) / (1000 * 60 * 60);
                            totalSalary += (hours * userHourlyRate);
                        }
                    }

                    // עדכון התצוגה על המסך
                    // יצירת אדפטר פנימי (SalaryAdapter) להצגת השורות בטבלה
                    adapter = new SalaryAdapter(shiftsList, userHourlyRate);
                    binding.rvSalaryShifts.setAdapter(adapter);

                    // הצגת הסיכום הכולל בתחתית המסך
                    binding.tvTotalSalary.setText(String.format(Locale.getDefault(), "₪%.2f", totalSalary));
                });
    }

    /**
     * הפונקציה המרכזית ליצירת ה-PDF.
     * הפונקציה "מציירת" את הדוח באופן ידני על דף וירטואלי.
     */
    private void generatePdfReport() {
        if (shiftsList.isEmpty()) {
            Toast.makeText(this, "אין נתונים לייצוא", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת מסמך PDF חדש בזיכרון
        PdfDocument pdfDocument = new PdfDocument();

        // הגדרת גודל דף (A4 סטנדרטי בפיקסלים: בערך 595x842)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        // התחלת עמוד מס' 1
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // Canvas הוא משטח הציור שלנו
        Canvas canvas = page.getCanvas();

        // Paint מגדיר את סגנון המכחול (צבע, גודל גופן, יישור)
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // --- שלב הציור (Drawing) ---

        // 1. ציור הכותרת
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.BLUE);
        titlePaint.setTextAlign(Paint.Align.CENTER); // מרכוז
        titlePaint.setFakeBoldText(true); // מודגש
        // ציור הטקסט במרכז הרוחב של הדף, בגובה 50 פיקסלים מלמעלה
        canvas.drawText("דוח שכר חודשי - ShiftSync", pageInfo.getPageWidth() / 2, 50, titlePaint);

        // 2. ציור פרטי העובד (בצד ימין - RTL)
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.RIGHT); // יישור לימין (לעברית)

        int startX = pageInfo.getPageWidth() - 50; // מיקום X: קצת שמאלה מהקצה הימני
        int startY = 100; // מיקום Y התחלתי

        canvas.drawText("שם העובד: " + userFullName, startX, startY, paint);
        canvas.drawText("תאריך הפקה: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), startX, startY + 20, paint);
        canvas.drawText("תעריף שעתי: " + userHourlyRate + " ₪", startX, startY + 40, paint);

        // 3. כותרות הטבלה
        startY += 80;
        paint.setFakeBoldText(true);
        // אנו מציירים את העמודות במרחקים קבועים מה-X ההתחלתי
        canvas.drawText("תאריך", startX, startY, paint);
        canvas.drawText("שעות", startX - 150, startY, paint);
        canvas.drawText("סכום", startX - 300, startY, paint);

        // ציור קו מפריד מתחת לכותרות
        paint.setFakeBoldText(false);
        canvas.drawLine(50, startY + 10, pageInfo.getPageWidth() - 50, startY + 10, paint);

        // 4. לולאה לציור שורות הטבלה
        startY += 40;
        double totalSum = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (Shift shift : shiftsList) {
            double hours = (double) (shift.getEndTime() - shift.getStartTime()) / (1000 * 60 * 60);
            double amount = hours * userHourlyRate;
            totalSum += amount;

            // ציור נתוני השורה
            canvas.drawText(sdf.format(shift.getStartTime()), startX, startY, paint);
            canvas.drawText(String.format("%.1f", hours), startX - 150, startY, paint);
            canvas.drawText(String.format("₪%.2f", amount), startX - 300, startY, paint);

            startY += 30; // ירידת שורה ב-30 פיקסלים
        }

        // 5. ציור סיכום סופי (מודגש וירוק)
        startY += 20;
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#4CAF50")); // ירוק
        paint.setTextSize(18);
        canvas.drawText("סה\"כ לתשלום: " + String.format("₪%.2f", totalSum), startX, startY, paint);

        // סיום העמוד
        pdfDocument.finishPage(page);

        // קריאה לפונקציה ששומרת את הקובץ פיזית
        savePdfToDownloads(pdfDocument);
    }

    /**
     * שמירת קובץ ה-PDF לתיקיית ההורדות (Downloads) של המכשיר.
     * שימוש ב-MediaStore (נתמך בגרסאות אנדרואיד חדשות - Scoped Storage).
     */
    private void savePdfToDownloads(PdfDocument pdfDocument) {
        // שם קובץ ייחודי (כולל חותמת זמן כדי למנוע דריסת קבצים)
        String fileName = "Salary_Report_" + System.currentTimeMillis() + ".pdf";

        // הגדרת המאפיינים של הקובץ
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");

        // באנדרואיד 10 ומעלה, מגדירים את הנתיב היחסי לתיקיית ההורדות
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        }

        // יצירת הקובץ במערכת הקבצים
        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);

        try {
            if (uri != null) {
                // פתיחת "צינור" (Stream) לכתיבה לתוך הקובץ שיצרנו
                OutputStream outputStream = getContentResolver().openOutputStream(uri);

                // כתיבת הנתונים הבינאריים של ה-PDF
                pdfDocument.writeTo(outputStream);

                // סגירה ושחרור משאבים
                if (outputStream != null) outputStream.close();
                pdfDocument.close();

                Toast.makeText(this, "הדוח נשמר בהצלחה בתיקיית ההורדות!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "שגיאה בשמירת הקובץ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}