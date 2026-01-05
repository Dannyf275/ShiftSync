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

/**
 * מסך ניהול בקשות (Manager Requests Screen).
 * מסך זה מציג למנהל את כל העובדים שביקשו להירשם למשמרות עתידיות (Pending).
 * המנהל יכול לאשר (להעביר ל-Assigned) או לדחות (להסיר מהרשימה).
 */
public class ShiftRequestsActivity extends AppCompatActivity {

    private ActivityShiftRequestsBinding binding;
    private FirebaseFirestore db;

    // האדפטר שמציג את רשימת הבקשות
    private RequestsAdapter adapter;

    // הרשימה של הבקשות (אובייקטי עזר המחברים בין משמרת לעובד)
    private List<ShiftRequestItem> requestItems;

    // אובייקט השומר את הרישום להאזנה. חשוב כדי שנוכל להפסיק להאזין כשהמסך נסגר.
    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShiftRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // אתחול Firebase
        db = FirebaseFirestore.getInstance();
        requestItems = new ArrayList<>();

        // הגדרת ה-RecyclerView
        binding.recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטר והגדרת הפעולות לכל כפתור
        adapter = new RequestsAdapter(requestItems, new RequestsAdapter.OnRequestActionListener() {
            @Override
            public void onApprove(ShiftRequestItem item) {
                // לחיצה על "אשר"
                approveRequest(item);
            }

            @Override
            public void onDeny(ShiftRequestItem item) {
                // לחיצה על "דחה"
                denyRequest(item);
            }
        });

        binding.recyclerViewRequests.setAdapter(adapter);

        // כפתור חזרה
        binding.btnBack.setOnClickListener(v -> finish());
    }

    /**
     * onStart נקרא כשהמסך הופך לגלו למשתמש.
     * זה הזמן המתאים להתחיל להאזין לשינויים ב-Database.
     */
    @Override
    protected void onStart() {
        super.onStart();
        startListeningForRequests();
    }

    /**
     * onStop נקרא כשהמסך מוסתר (למשל עברנו לאפליקציה אחרת).
     * חובה להפסיק את ההאזנה כדי לחסוך בסוללה ובתעבורת רשת.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (requestsListener != null) {
            requestsListener.remove(); // הסרת המאזין
        }
    }

    /**
     * הפונקציה המרכזית: טעינת הבקשות והאזנה לשינויים.
     */
    private void startListeningForRequests() {
        long now = System.currentTimeMillis();

        // שאילתה: תביא את כל המשמרות העתידיות.
        // אנו משתמשים ב-addSnapshotListener במקום get() כדי לקבל עדכונים חיים.
        requestsListener = db.collection("shifts")
                .whereGreaterThan("startTime", now)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ShiftRequests", "Error loading requests", error);
                        return;
                    }

                    // ניקוי הרשימה לפני טעינה מחדש (למניעת כפילויות)
                    requestItems.clear();

                    if (value != null) {
                        // מעבר על כל המשמרות שנמצאו
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Shift shift = doc.toObject(Shift.class);

                            // בדיקה: האם יש בכלל בקשות ממתינות למשמרת הזו?
                            if (shift != null && shift.getPendingUserIds() != null && !shift.getPendingUserIds().isEmpty()) {

                                // הנתונים שמורים ב-Firestore כשתי רשימות נפרדות (IDs ושמות).
                                // אנו עוברים עליהן ויוצרים אובייקטים מסוג ShiftRequestItem כדי שנוכל להציג אותם ברשימה שטוחה.
                                for (int i = 0; i < shift.getPendingUserIds().size(); i++) {
                                    String uid = shift.getPendingUserIds().get(i);

                                    // שליפת השם (עם הגנה מפני חריגה מגבולות המערך)
                                    String name = "עובד";
                                    if (shift.getPendingUserNames() != null && shift.getPendingUserNames().size() > i) {
                                        name = shift.getPendingUserNames().get(i);
                                    }

                                    // הוספה לרשימה לתצוגה
                                    requestItems.add(new ShiftRequestItem(shift, uid, name));
                                }
                            }
                        }
                    }

                    // עדכון התצוגה
                    adapter.notifyDataSetChanged();

                    // טיפול במצב של "אין בקשות" (Empty State)
                    if (requestItems.isEmpty()) {
                        binding.tvEmptyState.setVisibility(View.VISIBLE); // הצג הודעה "אין בקשות"
                    } else {
                        binding.tvEmptyState.setVisibility(View.GONE);    // הסתר הודעה
                    }
                });
    }

    /**
     * אישור בקשה.
     * הפעולה היא אטומית (Atomic): באותה פעולה אנו מסירים מה-Pending ומוסיפים ל-Assigned.
     */
    private void approveRequest(ShiftRequestItem item) {
        db.collection("shifts").document(item.getShift().getShiftId())
                .update(
                        // הסרה מרשימות ההמתנה
                        "pendingUserIds", FieldValue.arrayRemove(item.getUserId()),
                        "pendingUserNames", FieldValue.arrayRemove(item.getUserName()),
                        // הוספה לרשימות המשובצים
                        "assignedUserIds", FieldValue.arrayUnion(item.getUserId()),
                        "assignedUserNames", FieldValue.arrayUnion(item.getUserName())
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "אושר ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * דחיית בקשה.
     * מסירה את העובד מרשימת ההמתנה בלבד.
     */
    private void denyRequest(ShiftRequestItem item) {
        db.collection("shifts").document(item.getShift().getShiftId())
                .update(
                        // הסרה מרשימות ההמתנה (ללא הוספה לשיבוץ)
                        "pendingUserIds", FieldValue.arrayRemove(item.getUserId()),
                        "pendingUserNames", FieldValue.arrayRemove(item.getUserName())
                )
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "נדחה ❌", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}