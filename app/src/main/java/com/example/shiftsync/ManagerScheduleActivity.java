package com.example.shiftsync;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.databinding.ActivityManagerScheduleBinding;
import com.example.shiftsync.models.Shift;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ManagerScheduleActivity extends AppCompatActivity {

    private ActivityManagerScheduleBinding binding;
    private FirebaseFirestore db;
    private ShiftsAdapter adapter;
    private List<Shift> shiftsList;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagerScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        shiftsList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        setupRecyclerView();
        setupCalendar();
        loadShiftsForDate(selectedDate);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddShift.setOnClickListener(v -> showAddShiftDialog());
    }

    private void setupRecyclerView() {
        binding.rvShifts.setLayoutManager(new LinearLayoutManager(this));

        // שימוש בממשק המעודכן עם שני סוגי הלחיצות
        adapter = new ShiftsAdapter(shiftsList, new ShiftsAdapter.OnShiftClickListener() {
            @Override
            public void onDeleteClick(int position) {
                // מחיקת משמרת שלמה
                Shift shiftToDelete = shiftsList.get(position);
                deleteShift(shiftToDelete);
            }

            @Override
            public void onShiftClick(Shift shift) {
                // פתיחת רשימת העובדים במשמרת
                showShiftEmployeesDialog(shift);
            }
        });

        binding.rvShifts.setAdapter(adapter);
    }

    /**
     * פונקציה שמציגה את רשימת העובדים ומאפשרת הסרה.
     */
    private void showShiftEmployeesDialog(Shift shift) {
        // הכנת הנתונים לאדפטר של הדיאלוג
        List<ShiftEmployeesAdapter.EmployeeItem> employees = new ArrayList<>();

        if (shift.getAssignedUserIds() != null && !shift.getAssignedUserIds().isEmpty()) {
            for (int i = 0; i < shift.getAssignedUserIds().size(); i++) {
                String id = shift.getAssignedUserIds().get(i);
                String name = "עובד"; // ברירת מחדל

                // מנסים לקחת את השם מהמערך המקביל
                if (shift.getAssignedUserNames() != null && shift.getAssignedUserNames().size() > i) {
                    name = shift.getAssignedUserNames().get(i);
                }
                employees.add(new ShiftEmployeesAdapter.EmployeeItem(id, name));
            }
        }

        if (employees.isEmpty()) {
            Toast.makeText(this, "אין עובדים משובצים למשמרת זו", Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית הדיאלוג
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_shift_employees, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // הגדרת הרשימה בתוך הדיאלוג
        RecyclerView rv = view.findViewById(R.id.rvShiftEmployees);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ShiftEmployeesAdapter dialogAdapter = new ShiftEmployeesAdapter(employees, itemToRemove -> {
            // לוגיקה להסרת עובד
            removeEmployeeFromShift(shift, itemToRemove, dialog);
        });

        rv.setAdapter(dialogAdapter);

        // כפתור סגירה
        Button btnClose = view.findViewById(R.id.btnCloseDialog);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void removeEmployeeFromShift(Shift shift, ShiftEmployeesAdapter.EmployeeItem item, AlertDialog dialog) {
        // הסרת ה-ID והשם משני המערכים ב-Firestore
        db.collection("shifts").document(shift.getShiftId())
                .update(
                        "assignedUserIds", FieldValue.arrayRemove(item.id),
                        "assignedUserNames", FieldValue.arrayRemove(item.name)
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "העובד הוסר מהמשמרת", Toast.LENGTH_SHORT).show();
                    dialog.dismiss(); // סוגרים את הדיאלוג (הרשימה הראשית תתעדכן לבד בזכות ה-Listener)
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בהסרה", Toast.LENGTH_SHORT).show());
    }

    // --- שאר הפונקציות נשארות ללא שינוי (Calendar, Load, Add) ---

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            loadShiftsForDate(selectedDate);
        });
    }

    private void loadShiftsForDate(Calendar date) {
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.tvDateTitle.setText("משמרות לתאריך: " + sdf.format(date.getTime()));

        db.collection("shifts")
                .whereGreaterThanOrEqualTo("startTime", startOfDay.getTimeInMillis())
                .whereLessThanOrEqualTo("startTime", endOfDay.getTimeInMillis())
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    shiftsList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Shift shift = doc.toObject(Shift.class);
                            if (shift != null) shiftsList.add(shift);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddShiftDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shift, null);
        builder.setView(dialogView);

        EditText etStart = dialogView.findViewById(R.id.etDialogStartTime);
        EditText etEnd = dialogView.findViewById(R.id.etDialogEndTime);
        EditText etWorkers = dialogView.findViewById(R.id.etDialogRequiredWorkers);

        Calendar calStart = (Calendar) selectedDate.clone();
        Calendar calEnd = (Calendar) selectedDate.clone();

        etStart.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                calStart.set(Calendar.HOUR_OF_DAY, hour);
                calStart.set(Calendar.MINUTE, minute);
                etStart.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 8, 0, true).show();
        });

        etEnd.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                calEnd.set(Calendar.HOUR_OF_DAY, hour);
                calEnd.set(Calendar.MINUTE, minute);
                etEnd.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 16, 0, true).show();
        });

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String workersStr = etWorkers.getText().toString().trim();
            if (workersStr.isEmpty()) return;
            if (calEnd.getTimeInMillis() <= calStart.getTimeInMillis()) {
                Toast.makeText(this, "זמנים לא תקינים", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int requiredWorkers = Integer.parseInt(workersStr);
                String id = UUID.randomUUID().toString();
                Shift newShift = new Shift(id, calStart.getTimeInMillis(), calEnd.getTimeInMillis(), requiredWorkers);

                db.collection("shifts").document(id).set(newShift)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "נוסף בהצלחה", Toast.LENGTH_SHORT).show());
            } catch (NumberFormatException e) { }
        });

        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void deleteShift(Shift shift) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת משמרת")
                .setMessage("האם למחוק את המשמרת לגמרי?")
                .setPositiveButton("כן", (dialog, which) -> {
                    db.collection("shifts").document(shift.getShiftId()).delete();
                })
                .setNegativeButton("לא", null)
                .show();
    }
}