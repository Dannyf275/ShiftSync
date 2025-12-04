package com.example.shiftsync;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

// שים לב: וודא שיש לך קובץ XML מתאים עם השם הזה
import com.example.shiftsync.databinding.ActivityShiftRequestsBinding;
import com.example.shiftsync.models.Shift;
import com.example.shiftsync.models.ShiftRequestItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ShiftRequestsActivity extends AppCompatActivity {

    private ActivityShiftRequestsBinding binding;
    private FirebaseFirestore db;
    private RequestsAdapter adapter;
    private List<ShiftRequestItem> requestItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShiftRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        requestItems = new ArrayList<>();

        binding.recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר והגדרת הפעולות
        adapter = new RequestsAdapter(requestItems, new RequestsAdapter.OnRequestActionListener() {
            @Override
            public void onApprove(ShiftRequestItem item) {
                approveRequest(item);
            }

            @Override
            public void onDeny(ShiftRequestItem item) {
                denyRequest(item);
            }
        });

        binding.recyclerViewRequests.setAdapter(adapter);

        // כפתור חזרה (אם קיים ב-XML)
        if (binding.btnBack != null) {
            binding.btnBack.setOnClickListener(v -> finish());
        }

        loadRequests();
    }

    /**
     * טעינת כל הבקשות הממתינות.
     */
    private void loadRequests() {
        long now = System.currentTimeMillis();

        // שליפת משמרות עתידיות בלבד
        db.collection("shifts")
                .whereGreaterThan("startTime", now)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    requestItems.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Shift shift = doc.toObject(Shift.class);

                        // בדיקה: האם יש למשמרת הזו בקשות ממתינות?
                        if (shift != null && shift.getPendingUserIds() != null && !shift.getPendingUserIds().isEmpty()) {

                            // פירוק רשימת הממתינים לפריטים בודדים
                            for (int i = 0; i < shift.getPendingUserIds().size(); i++) {
                                String uid = shift.getPendingUserIds().get(i);
                                // מנסים לשלוף שם, אם קיים
                                String name = "עובד";
                                if (shift.getPendingUserNames() != null && shift.getPendingUserNames().size() > i) {
                                    name = shift.getPendingUserNames().get(i);
                                }

                                // הוספה לרשימה השטוחה
                                requestItems.add(new ShiftRequestItem(shift, uid, name));
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // טיפול בהודעת "אין בקשות"
                    if (requestItems.isEmpty()) {
                        Toast.makeText(this, "אין בקשות ממתינות", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * אישור בקשה: העברה מ-Pending ל-Assigned.
     */
    private void approveRequest(ShiftRequestItem item) {
        db.collection("shifts").document(item.getShift().getShiftId())
                .update(
                        // הסרה מרשימת הממתינים
                        "pendingUserIds", FieldValue.arrayRemove(item.getUserId()),
                        "pendingUserNames", FieldValue.arrayRemove(item.getUserName()),
                        // הוספה לרשימת המאושרים
                        "assignedUserIds", FieldValue.arrayUnion(item.getUserId()),
                        "assignedUserNames", FieldValue.arrayUnion(item.getUserName())
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הבקשה אושרה!", Toast.LENGTH_SHORT).show();
                    loadRequests(); // רענון הרשימה
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה באישור", Toast.LENGTH_SHORT).show());
    }

    /**
     * דחיית בקשה: רק הסרה מ-Pending.
     */
    private void denyRequest(ShiftRequestItem item) {
        db.collection("shifts").document(item.getShift().getShiftId())
                .update(
                        "pendingUserIds", FieldValue.arrayRemove(item.getUserId()),
                        "pendingUserNames", FieldValue.arrayRemove(item.getUserName())
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הבקשה נדחתה", Toast.LENGTH_SHORT).show();
                    loadRequests(); // רענון הרשימה
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בדחייה", Toast.LENGTH_SHORT).show());
    }
}