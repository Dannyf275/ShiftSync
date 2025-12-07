package com.example.shiftsync;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shiftsync.databinding.ActivityEmployeesListBinding;
import com.example.shiftsync.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך זה מציג למנהל את רשימת כל העובדים הרשומים במערכת.
 * הוא מאפשר למנהל לצפות בפרטים ולערוך נתונים כמו שכר ות"ז.
 */
public class EmployeesListActivity extends AppCompatActivity {

    // משתנה לגישה לרכיבי ה-XML (ViewBinding)
    private ActivityEmployeesListBinding binding;

    // חיבור למסד הנתונים של Firebase
    private FirebaseFirestore db;

    // האדפטר שמחבר בין רשימת הנתונים (List<User>) לרכיב התצוגה (RecyclerView)
    private EmployeesAdapter adapter;

    // הרשימה שמחזיקה את נתוני העובדים בזיכרון
    private List<User> employeesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול ה-ViewBinding
        binding = ActivityEmployeesListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול Firebase ורשימת הנתונים
        db = FirebaseFirestore.getInstance();
        employeesList = new ArrayList<>();

        // 3. הגדרת התצוגה של הרשימה (לינארית - שורה אחרי שורה)
        binding.recyclerViewEmployees.setLayoutManager(new LinearLayoutManager(this));

        // 4. יצירת האדפטר והגדרת מה קורה כשלוחצים על "ערוך"
        // הפרמטר user מייצג את העובד הספציפי שנלחץ בשורה
        adapter = new EmployeesAdapter(employeesList, user -> {
            showEditDialog(user); // פתיחת החלון הקופץ לעריכה
        });

        // חיבור האדפטר לרשימה הגרפית
        binding.recyclerViewEmployees.setAdapter(adapter);

        // 5. כפתור חזרה למסך הקודם
        binding.btnBack.setOnClickListener(v -> finish());

        // 6. טעינת הנתונים הראשונית מהענן
        loadEmployees();
    }

    /**
     * פונקציה שמציגה חלון קופץ (Dialog) לעריכת פרטי העובד.
     * @param user העובד שאת פרטיו רוצים לערוך.
     */
    private void showEditDialog(User user) {
        // בניית הדיאלוג
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // "ניפוח" (Inflation) של קובץ ה-XML של הדיאלוג לתוך אובייקט View
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_employee, null);
        builder.setView(view);

        // קישור לשדות שבתוך הדיאלוג
        EditText etName = view.findViewById(R.id.etEditName);
        EditText etId = view.findViewById(R.id.etEditIdNumber);
        EditText etRate = view.findViewById(R.id.etEditHourlyRate);

        // מילוי הנתונים הקיימים בתוך השדות (כדי שהמנהל יראה מה יש כרגע)
        etName.setText(user.getFullName());
        if (user.getIdNumber() != null) {
            etId.setText(user.getIdNumber());
        }
        etRate.setText(String.valueOf(user.getHourlyRate()));

        // הגדרת כפתור "שמור שינויים"
        builder.setPositiveButton("שמור שינויים", (dialog, which) -> {
            // שליפת הנתונים החדשים שהוזנו
            String newName = etName.getText().toString().trim();
            String newId = etId.getText().toString().trim();
            String newRateStr = etRate.getText().toString().trim();

            // בדיקות תקינות (Validations)
            if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newRateStr)) {
                Toast.makeText(this, "נא למלא שם ושכר", Toast.LENGTH_SHORT).show();
                return;
            }

            // המרה של השכר ממחרוזת למספר עשרוני
            try {
                double newRate = Double.parseDouble(newRateStr);

                // קריאה לפונקציה שמעדכנת ב-Firebase
                updateEmployeeInFirestore(user.getUid(), newName, newId, newRate);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "שכר חייב להיות מספר תקין", Toast.LENGTH_SHORT).show();
            }
        });

        // כפתור ביטול - סוגר את הדיאלוג
        builder.setNegativeButton("ביטול", null);

        // הצגת הדיאלוג
        builder.show();
    }

    /**
     * פונקציה שמעדכנת את פרטי העובד ב-Firestore.
     * משתמשת ב-update() ולא ב-set() כדי לא לדרוס שדות אחרים שלא שינינו (כמו אימייל).
     */
    private void updateEmployeeInFirestore(String uid, String name, String idNum, double rate) {
        // יצירת מפה (Map) שמכילה רק את השדות שאנחנו רוצים לשנות
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("idNumber", idNum);
        updates.put("hourlyRate", rate);

        // שליחת העדכון ל-Firebase
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הפרטים עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    // רענון הרשימה כדי לראות את השינויים מיד
                    loadEmployees();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * פונקציה לטעינת רשימת העובדים מ-Firestore.
     * שולפת רק משתמשים שהתפקיד שלהם הוא 'employee'.
     */
    private void loadEmployees() {
        db.collection("users")
                .whereEqualTo("role", User.ROLE_EMPLOYEE) // סינון לפי תפקיד
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // ניקוי הרשימה הישנה כדי למנוע כפילויות
                    employeesList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        // מעבר על כל המסמכים שהתקבלו
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            User user = document.toObject(User.class); // המרת JSON לאובייקט User
                            if (user != null) {
                                employeesList.add(user);
                            }
                        }
                        // עדכון האדפטר והסתרת הודעת "אין עובדים"
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyState.setVisibility(View.GONE);
                    } else {
                        // אם אין תוצאות -> הצגת הודעת "אין עובדים"
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}