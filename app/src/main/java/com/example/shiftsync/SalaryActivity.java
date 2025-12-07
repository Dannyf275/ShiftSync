package com.example.shiftsync;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// חשוב: זה האימפורט של ה-Binding שנוצר מה-XML
import com.example.shiftsync.databinding.ActivitySalaryBinding;
import com.example.shiftsync.models.Shift;
import com.example.shiftsync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SalaryActivity extends AppCompatActivity {

    private ActivitySalaryBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private double userHourlyRate = 0.0; // נשמור את התעריף

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול ה-Binding
        binding = ActivitySalaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // הגדרת כפתור חזרה
        binding.btnBack.setOnClickListener(v -> finish());

        // התחלת תהליך הטעינה
        loadUserDataAndCalculate();
    }

    private void loadUserDataAndCalculate() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // 1. שליפת פרטי המשתמש כדי לקבל את השכר השעתי
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            userHourlyRate = user.getHourlyRate();
                            binding.tvHourlyRate.setText("תעריף: " + userHourlyRate + " ₪");

                            // 2. אחרי שיש תעריף -> מחשבים משמרות
                            calculateSalary(uid);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת משתמש", Toast.LENGTH_SHORT).show());
    }

    private void calculateSalary(String uid) {
        // שליפת כל המשמרות שהמשתמש אושר בהן (assigned)
        db.collection("shifts")
                .whereArrayContains("assignedUserIds", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalHours = 0;
                    StringBuilder detailsBuilder = new StringBuilder();
                    long currentTime = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Shift shift = doc.toObject(Shift.class);

                        // חישוב רק למשמרות מהעבר (שכבר בוצעו)
                        if (shift != null && shift.getEndTime() < currentTime) {

                            // חישוב הפרש זמנים במילי-שניות
                            long durationMillis = shift.getEndTime() - shift.getStartTime();
                            // המרה לשעות (כולל שברים עשרוניים)
                            double hours = (double) durationMillis / (1000 * 60 * 60);

                            totalHours += hours;

                            // עיצוב הטקסט לתצוגה
                            SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

                            detailsBuilder.append("📅 ").append(dateFmt.format(shift.getStartTime()))
                                    .append("  ⏰ ").append(timeFmt.format(shift.getStartTime()))
                                    .append(" - ").append(timeFmt.format(shift.getEndTime()))
                                    .append("\n⏳ שעות: ").append(String.format("%.2f", hours))
                                    .append("\n--------------------------------\n");
                        }
                    }

                    // חישוב השכר הסופי
                    double totalMoney = totalHours * userHourlyRate;

                    // עדכון המסך
                    binding.tvTotalHours.setText(String.format("סה''כ שעות: %.2f", totalHours));
                    binding.tvTotalSalary.setText(String.format("₪%.2f", totalMoney));

                    if (detailsBuilder.length() > 0) {
                        binding.tvShiftsDetails.setText(detailsBuilder.toString());
                    } else {
                        binding.tvShiftsDetails.setText("לא נמצאו משמרות שהסתיימו.");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בחישוב שכר", Toast.LENGTH_SHORT).show());
    }
}