package com.example.shiftsync;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shiftsync.databinding.ActivityShiftRequestsBinding;
import com.example.shiftsync.models.Shift;
import com.example.shiftsync.models.ShiftRequestItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ShiftRequestsActivity extends AppCompatActivity {

    private ActivityShiftRequestsBinding binding;
    private FirebaseFirestore db;
    private RequestsAdapter adapter;
    private List<ShiftRequestItem> requestItems;
    private ListenerRegistration requestsListener; // משתנה לשמירת ההאזנה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShiftRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        requestItems = new ArrayList<>();

        binding.recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));

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

        binding.btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // מתחילים להאזין כשנכנסים למסך
        startListeningForRequests();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // מפסיקים להאזין כשיוצאים (חוסך סוללה ומונע קריסות)
        if (requestsListener != null) {
            requestsListener.remove();
        }
    }

    private void startListeningForRequests() {
        long now = System.currentTimeMillis();

        // שינוי קריטי: שימוש ב-addSnapshotListener במקום get()
        requestsListener = db.collection("shifts")
                .whereGreaterThan("startTime", now) // רק משמרות עתידיות
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ShiftRequests", "Error loading requests", error);
                        return;
                    }

                    requestItems.clear();

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Shift shift = doc.toObject(Shift.class);

                            // בדיקה: האם יש בקשות ממתינות?
                            if (shift != null && shift.getPendingUserIds() != null && !shift.getPendingUserIds().isEmpty()) {

                                // לולאה על כל הבקשות בתוך המשמרת
                                for (int i = 0; i < shift.getPendingUserIds().size(); i++) {
                                    String uid = shift.getPendingUserIds().get(i);

                                    // ניסיון לשליפת שם בטוחה
                                    String name = "עובד";
                                    if (shift.getPendingUserNames() != null && shift.getPendingUserNames().size() > i) {
                                        name = shift.getPendingUserNames().get(i);
                                    }

                                    requestItems.add(new ShiftRequestItem(shift, uid, name));
                                }
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();

                    // הצגה/הסתרה של הודעת "אין בקשות"
                    if (requestItems.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmptyState.setVisibility(View.GONE);
                    }
                });
    }

    private void approveRequest(ShiftRequestItem item) {
        // לוגיקה לאישור: הסרה מ-Pending והוספה ל-Assigned
        db.collection("shifts").document(item.getShift().getShiftId())
                .update(
                        "pendingUserIds", FieldValue.arrayRemove(item.getUserId()),
                        "pendingUserNames", FieldValue.arrayRemove(item.getUserName()),
                        "assignedUserIds", FieldValue.arrayUnion(item.getUserId()),
                        "assignedUserNames", FieldValue.arrayUnion(item.getUserName())
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "אושר ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void denyRequest(ShiftRequestItem item) {
        // לוגיקה לדחייה: רק הסרה מ-Pending
        db.collection("shifts").document(item.getShift().getShiftId())
                .update(
                        "pendingUserIds", FieldValue.arrayRemove(item.getUserId()),
                        "pendingUserNames", FieldValue.arrayRemove(item.getUserName())
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "נדחה ❌", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}