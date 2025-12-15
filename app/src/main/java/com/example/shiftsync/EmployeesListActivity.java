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
import com.google.firebase.firestore.ListenerRegistration; // ייבוא חשוב לניהול המאזין

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מסך ניהול עובדים (עבור המנהל).
 * מציג רשימה חיה של כל העובדים במערכת.
 * מאפשר עריכת פרטים (שם, ת"ז, שכר).
 */
public class EmployeesListActivity extends AppCompatActivity {

    private ActivityEmployeesListBinding binding;
    private FirebaseFirestore db;
    private EmployeesAdapter adapter;
    private List<User> employeesList;

    // משתנה שמחזיק את "צינור הנתונים" הפתוח מול Firebase.
    // אנחנו צריכים לשמור אותו כדי שנוכל לסגור אותו כשיוצאים מהמסך.
    private ListenerRegistration employeesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אתחול ViewBinding
        binding = ActivityEmployeesListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. אתחול Firebase ורשימות
        db = FirebaseFirestore.getInstance();
        employeesList = new ArrayList<>();

        // 3. הגדרת ה-RecyclerView
        binding.recyclerViewEmployees.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר והגדרת פעולת העריכה (בלחיצה על כפתור בעיפרון)
        adapter = new EmployeesAdapter(employeesList, user -> {
            showEditDialog(user);
        });

        binding.recyclerViewEmployees.setAdapter(adapter);

        // 4. כפתור חזרה
        binding.btnBack.setOnClickListener(v -> finish());
    }

    // --- מחזור חיים של Activity (Lifecycle) ---

    @Override
    protected void onStart() {
        super.onStart();
        // כשהמסך עולה ונהיה גלוי למשתמש -> מתחילים להאזין לשינויים
        startListeningToEmployees();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // כשהמסך יורד לרקע או נסגר -> מנתקים את ההאזנה כדי לא לבזבז משאבים
        if (employeesListener != null) {
            employeesListener.remove();
            employeesListener = null;
        }
    }

    /**
     * פונקציה להאזנה בזמן אמת לרשימת העובדים.
     * כל שינוי ב-DB (הוספה, מחיקה, עריכה) יפעיל את הקוד הזה אוטומטית.
     */
    private void startListeningToEmployees() {
        employeesListener = db.collection("users")
                .whereEqualTo("role", User.ROLE_EMPLOYEE) // סינון: רק עובדים
                .addSnapshotListener((value, error) -> {
                    // טיפול בשגיאות חיבור
                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ניקוי הרשימה הישנה כדי למנוע כפילויות
                    employeesList.clear();

                    if (value != null && !value.isEmpty()) {
                        // מעבר על כל המסמכים שהתקבלו
                        for (DocumentSnapshot document : value.getDocuments()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                employeesList.add(user);
                            }
                        }
                        // עדכון האדפטר שיש מידע חדש
                        adapter.notifyDataSetChanged();

                        // הסתרת הודעת "אין עובדים"
                        binding.tvEmptyState.setVisibility(View.GONE);
                    } else {
                        // אם מחקנו את כל העובדים -> הרשימה ריקה
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    /**
     * הצגת דיאלוג לעריכת פרטי עובד.
     */
    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_employee, null);
        builder.setView(view);

        // קישור לשדות בדיאלוג
        EditText etName = view.findViewById(R.id.etEditName);
        EditText etId = view.findViewById(R.id.etEditIdNumber);
        EditText etRate = view.findViewById(R.id.etEditHourlyRate);

        // מילוי נתונים קיימים
        etName.setText(user.getFullName());
        if (user.getIdNumber() != null) etId.setText(user.getIdNumber());
        etRate.setText(String.valueOf(user.getHourlyRate()));

        // כפתור שמירה
        builder.setPositiveButton("שמור שינויים", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newId = etId.getText().toString().trim();
            String newRateStr = etRate.getText().toString().trim();

            if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newRateStr)) {
                Toast.makeText(this, "נא למלא שם ושכר", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double newRate = Double.parseDouble(newRateStr);
                // עדכון ב-DB
                updateEmployeeInFirestore(user.getUid(), newName, newId, newRate);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "שכר חייב להיות מספר", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    /**
     * ביצוע העדכון ב-Firestore.
     */
    private void updateEmployeeInFirestore(String uid, String name, String idNum, double rate) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("idNumber", idNum);
        updates.put("hourlyRate", rate);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "הפרטים עודכנו", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}