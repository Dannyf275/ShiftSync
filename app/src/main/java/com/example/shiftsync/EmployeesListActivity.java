package com.example.shiftsync;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shiftsync.databinding.ActivityEmployeesListBinding;
import com.example.shiftsync.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeesListActivity extends AppCompatActivity {

    private ActivityEmployeesListBinding binding;
    private FirebaseFirestore db;
    private EmployeesAdapter adapter;
    private List<User> employeesList;
    private ListenerRegistration employeesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeesListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        employeesList = new ArrayList<>();

        binding.recyclerViewEmployees.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeesAdapter(employeesList, user -> {
            showEditDialog(user);
        });

        binding.recyclerViewEmployees.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListeningToEmployees();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (employeesListener != null) {
            employeesListener.remove();
        }
    }

    private void startListeningToEmployees() {
        employeesListener = db.collection("users")
                .whereEqualTo("role", User.ROLE_EMPLOYEE)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    employeesList.clear();
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                employeesList.add(user);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyState.setVisibility(View.GONE);
                    } else {
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    /**
     * פונקציה שמציגה את דיאלוג העריכה והמחיקה
     */
    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_employee, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etEditName);
        EditText etId = view.findViewById(R.id.etEditIdNumber);
        EditText etRate = view.findViewById(R.id.etEditHourlyRate);
        Button btnDelete = view.findViewById(R.id.btnDeleteEmployee);

        // מילוי נתונים
        etName.setText(user.getFullName());
        if (user.getIdNumber() != null) etId.setText(user.getIdNumber());
        etRate.setText(String.valueOf(user.getHourlyRate()));

        AlertDialog dialog = builder.create();

        // --- כפתור מחיקה ---
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("מחיקת עובד")
                    .setMessage("האם למחוק את העובד מהרשימה?\n(פעולה זו תאפשר לך ליצור אותו מחדש עם סיסמה חדשה)")
                    .setPositiveButton("כן, מחק", (d, w) -> {
                        // מחיקה מ-Firestore
                        db.collection("users").document(user.getUid()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "העובד נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("ביטול", null)
                    .show();
        });

        // --- כפתור שמירה (מובנה בדיאלוג) ---
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "שמור שינויים", (d, which) -> {
            // הטיפול בלחיצה כדי למנוע סגירה אוטומטית במקרה של שגיאה נעשה בנפרד,
            // אך לצורך הפשטות כאן נשתמש במימוש הסטנדרטי
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "ביטול", (d, w) -> dialog.dismiss());

        dialog.show();

        // הגדרת מאזין ל"שמור" אחרי שהדיאלוג הוצג (כדי למנוע סגירה אוטומטית אם חסר מידע)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newId = etId.getText().toString().trim();
            String newRateStr = etRate.getText().toString().trim();

            if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newRateStr)) {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    double newRate = Double.parseDouble(newRateStr);
                    updateEmployeeInFirestore(user.getUid(), newName, newId, newRate);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "שכר לא תקין", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateEmployeeInFirestore(String uid, String name, String idNum, double rate) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("idNumber", idNum);
        updates.put("hourlyRate", rate);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "עודכן בהצלחה", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בעדכון", Toast.LENGTH_SHORT).show());
    }
}