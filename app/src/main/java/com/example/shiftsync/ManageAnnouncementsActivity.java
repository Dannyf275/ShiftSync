package com.example.shiftsync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.Announcement;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//מסך ניהול הודעות
public class ManageAnnouncementsActivity extends AppCompatActivity {

    // אובייקט לחיבור למסד הנתונים
    private FirebaseFirestore db;

    // רכיבי התצוגה
    private RecyclerView rvAnnouncements; // הרשימה הנגללת
    private FloatingActionButton fabAdd;    // הכפתור העגול המרחף (+) להוספה

    // ניהול הנתונים והתצוגה
    private AnnouncementsAdapter adapter;
    private List<Announcement> list;

    // משתנה לשמירת שם המנהל שמפרסם את ההודעה (ברירת מחדל "Manager" עד שייטען השם האמיתי)
    private String currentManagerName = "Manager";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_announcements); // טעינת קובץ העיצוב

        // אתחול המסד נתונים
        db = FirebaseFirestore.getInstance();

        // קריאה לפונקציה שטוענת את שם המנהל הנוכחי
        loadManagerName();

        // קישור לרכיבים ב-XML
        rvAnnouncements = findViewById(R.id.rvAnnouncements);
        fabAdd = findViewById(R.id.fabAddAnnouncement);

        // הגדרת כפתור חזרה למסך הקודם
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // אתחול הרשימה והגדרת ה-RecyclerView
        list = new ArrayList<>();
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר.
        // הפרמטר השני הוא "Lambda Expression" שמגדיר מה קורה כשלוחצים על כפתור המחיקה בשורה.
        // אנחנו מעבירים פונקציה שתקרא ל-deleteAnnouncement עם הפריט שנבחר.
        adapter = new AnnouncementsAdapter(list, item -> deleteAnnouncement(item));
        rvAnnouncements.setAdapter(adapter);

        // הגדרת לחיצה על כפתור הפלוס (+) -> פתיחת דיאלוג הוספה
        fabAdd.setOnClickListener(v -> showAddDialog());

        // התחלת האזנה לשינויים במסד הנתונים וטעינת ההודעות
        loadAnnouncements();
    }

    //טעינת שם המנהל המחובר
    private void loadManagerName() {
        // בדיקה שאכן יש משתמש מחובר
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // שליפת פרטי המשתמש מאוסף "users"
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            if (d.exists() && d.getString("fullName") != null) {
                currentManagerName = d.getString("fullName");
            }
        });
    }

    //טעינת רשימת ההודעות מהפיירבייס
    private void loadAnnouncements() {
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING) // מיון: מהחדש לישן
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינת הודעות", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    list.clear(); // ניקוי הרשימה הישנה למניעת כפילויות

                    if (value != null) {
                        // המרת כל מסמך לאובייקט Announcement והוספה לרשימה
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Announcement announcement = doc.toObject(Announcement.class);
                            if (announcement != null) list.add(announcement);
                        }
                    }
                    // עדכון התצוגה
                    adapter.notifyDataSetChanged();
                });
    }

    //חלון הוספת הודעה חדשה
    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // "ניפוח" העיצוב המיוחד של הדיאלוג (dialog_add_announcement.xml)
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_announcement, null);
        builder.setView(view);

        // קישור לשדות הקלט בתוך הדיאלוג
        EditText etTitle = view.findViewById(R.id.etAnnTitle);
        EditText etContent = view.findViewById(R.id.etAnnContent);

        // הגדרת כפתור "פרסם"
        builder.setPositiveButton("פרסם", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            // בדיקת תקינות: האם השדות מלאים?
            if (!title.isEmpty() && !content.isEmpty()) {
                // יצירת מזהה ייחודי (Random ID) להודעה
                String id = UUID.randomUUID().toString();
                long now = System.currentTimeMillis(); // זמן הנוכחי

                // יצירת האובייקט
                Announcement announcement = new Announcement(id, title, content, now, currentManagerName);

                // שמירה במסד הנתונים תחת ה-ID שיצרנו
                db.collection("announcements").document(id).set(announcement)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "ההודעה פורסמה בהצלחה", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בפרסום", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            }
        });

        // כפתור ביטול סוגר את הדיאלוג
        builder.setNegativeButton("ביטול", null);

        builder.show(); // הצגת הדיאלוג למסך
    }

    //מחיקת הודעה
    private void deleteAnnouncement(Announcement item) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת הודעה")
                .setMessage("האם למחוק את ההודעה: " + item.getTitle() + "?")
                .setPositiveButton("כן", (d, w) -> {
                    // ביצוע המחיקה ב-Firestore לפי ה-ID של ההודעה
                    db.collection("announcements").document(item.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "נמחק", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("לא", null)
                .show();
    }
}