package com.example.shiftsync;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
// שים לב: חשוב לייבא את ה-SearchView הנכון (של AndroidX) לתאימות
import androidx.appcompat.widget.SearchView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך ניהול העובדים (עבור המנהל).
 * מציג רשימה של כל המשתמשים שהם בתפקיד "employee".
 * כולל יכולות חיפוש (סינון מקומי), מחיקה, ועריכת שכר.
 */
public class EmployeesListActivity extends AppCompatActivity {

    // אובייקט לחיבור למסד הנתונים
    private FirebaseFirestore db;

    // רכיבי הממשק (UI)
    private RecyclerView rvEmployees;   // הרשימה הויזואלית
    private EmployeesAdapter adapter;   // האדפטר שמקשר בין המידע לתצוגה
    private SearchView searchView;      // שורת החיפוש

    // רשימה שמחזיקה את *כל* העובדים שנטענו מהשרת.
    // אנחנו שומרים אותה בצד כדי שנוכל לסנן ממנה תוצאות בלי לבקש שוב מהשרת בכל אות שמקלידים.
    private List<User> fullList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employees_list);

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();

        // קישור לרכיבים בקובץ ה-XML
        rvEmployees = findViewById(R.id.rvEmployees);
        searchView = findViewById(R.id.searchView);

        // כפתור חזרה למסך הראשי
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // אתחול הרשימה המלאה
        fullList = new ArrayList<>();

        // הגדרת תצוגת הרשימה (טור אחד אנכי)
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר והגדרת הפעולות (Callbacks) ללחיצות על כפתורים בתוך כל שורה
        adapter = new EmployeesAdapter(new ArrayList<>(), new EmployeesAdapter.OnEmployeeClickListener() {
            @Override
            public void onDeleteClick(User user) {
                // לחיצה על "מחק" -> הפעלת פונקציית המחיקה
                deleteEmployee(user);
            }
            @Override
            public void onEditClick(User user) {
                // לחיצה על "ערוך" -> הפעלת דיאלוג עריכת שכר
                showEditRateDialog(user);
            }
        });

        // חיבור האדפטר לרשימה
        rvEmployees.setAdapter(adapter);

        // טעינת הנתונים מהשרת
        loadEmployees();

        // הגדרת מנגנון החיפוש
        setupSearch();
    }

    /**
     * טעינת רשימת העובדים מ-Firestore.
     * אנו מאזינים לשינויים בזמן אמת (addSnapshotListener), כך שאם עובד נרשם עכשיו,
     * הוא יופיע מיד ברשימה אצל המנהל.
     */
    private void loadEmployees() {
        db.collection("users")
                .whereEqualTo("role", User.ROLE_EMPLOYEE) // סינון: רק עובדים (לא מנהלים)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return; // במקרה של שגיאה, יוצאים

                    fullList.clear(); // מנקים את הרשימה הישנה

                    if (value != null) {
                        // המרה של כל מסמך לאובייקט User והוספה לרשימה המלאה
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            fullList.add(doc.toObject(User.class));
                        }
                    }
                    // טעינה ראשונית מציגה את כולם (ללא סינון)
                    adapter.updateList(fullList);
                });
    }

    /**
     * הגדרת המאזין לשורת החיפוש.
     * הפונקציה מגיבה לכל שינוי בטקסט שהמשתמש מקליד.
     */
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // נקרא כשלוחצים על "Enter" במקלדת
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // נקרא בכל פעם שמקלידים/מוחקים אות
                filter(newText);
                return false;
            }
        });
    }

    /**
     * פונקציית הסינון המקומי (Client-side Filtering).
     * עוברת על הרשימה המלאה (`fullList`) ויוצרת רשימה חדשה רק עם התוצאות המתאימות.
     * @param text - הטקסט לחיפוש.
     */
    private void filter(String text) {
        List<User> filteredList = new ArrayList<>();

        for (User user : fullList) {
            // תנאי החיפוש: האם השם מכיל את הטקסט? או האם הת"ז מכילה את המספר?
            // (toLowerCase מאפשר חיפוש ללא חשיבות לאותיות גדולות/קטנות באנגלית)
            if (user.getFullName().toLowerCase().contains(text.toLowerCase()) ||
                    user.getIdNumber().contains(text)) {
                filteredList.add(user);
            }
        }

        // עדכון האדפטר עם הרשימה המסוננת בלבד
        adapter.updateList(filteredList);
    }

    /**
     * מחיקת עובד מהמערכת.
     * מציג דיאלוג אישור לפני המחיקה בפועל.
     */
    private void deleteEmployee(User user) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת עובד")
                .setMessage("למחוק את " + user.getFullName() + "?")
                .setPositiveButton("כן", (d, w) -> {
                    // ביצוע המחיקה ב-Firestore
                    db.collection("users").document(user.getUid()).delete();
                })
                .setNegativeButton("לא", null)
                .show();
    }

    /**
     * הצגת דיאלוג לעריכת שכר שעתי.
     * יוצר תיבת טקסט (EditText) בתוך חלון קופץ (AlertDialog).
     */
    private void showEditRateDialog(User user) {
        // יצירת שדה הקלט באופן דינמי (בקוד)
        EditText input = new EditText(this);
        input.setHint("שכר שעתי חדש");
        // הגבלה למספרים בלבד (כולל עשרוני)
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(user.getHourlyRate())); // הצגת השכר הנוכחי

        new AlertDialog.Builder(this)
                .setTitle("עדכון שכר לעובד: " + user.getFullName())
                .setView(input) // הכנסת שדה הקלט לדיאלוג
                .setPositiveButton("עדכן", (d, w) -> {
                    try {
                        // המרת הקלט למספר ועדכון ב-Firestore
                        double newRate = Double.parseDouble(input.getText().toString());
                        db.collection("users").document(user.getUid()).update("hourlyRate", newRate);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "ערך לא תקין", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}