package com.example.shiftsync.models;

import java.util.ArrayList;
import java.util.List;

/**
 * מחלקה המייצגת "משמרת" (Shift) במערכת.
 * המחלקה משמשת למיפוי נתונים מול Firebase Firestore (אוסף "shifts").
 * היא מכילה מידע על הזמנים, העובדים המשובצים, והבקשות הממתינות.
 */
public class Shift {

    // --- שדות הנתונים (Fields) ---

    // מזהה ייחודי של המשמרת (בדרך כלל UUID או מזהה אוטומטי של Firebase).
    // משמש למציאת המשמרת לצורך עדכון, מחיקה או שיבוץ עובדים.
    private String shiftId;

    // זמן התחלה במילי-שניות (Epoch Time).
    // נשמר כ-long כדי לאפשר חישובי זמנים (כמו משך משמרת) ומיון כרונולוגי קל.
    private long startTime;

    // זמן סיום במילי-שניות.
    private long endTime;

    // מספר העובדים הנדרש למשמרת זו (מכסת עובדים).
    // המערכת תשתמש בזה כדי לחשב אם המשמרת "מלאה" או לא.
    private int requiredWorkers;

    // רשימת המזהים (UID) של העובדים שכבר שובצו ואושרו למשמרת.
    private List<String> assignedUserIds;

    // רשימת השמות של העובדים המשובצים (לצורך תצוגה מהירה בלי לשלוף שוב את פרטי המשתמש).
    private List<String> assignedUserNames;

    // רשימת מזהים (UID) של עובדים שביקשו להירשם אך טרם אושרו (Pending).
    private List<String> pendingUserIds;

    // רשימת שמות של העובדים הממתינים לאישור.
    private List<String> pendingUserNames;

    // --- שדה חדש: הערות מנהל ---
    // טקסט חופשי שהמנהל יכול להוסיף (למשל: "חובה להגיע עם נעליים סגורות").
    private String notes;

    // --- בנאים (Constructors) ---

    /**
     * בנאי ריק (Empty Constructor).
     * חובה! Firebase משתמש בבנאי הזה כדי ליצור מופע של המחלקה
     * לפני שהוא מזרים לתוכה את הנתונים מהמסד (JSON).
     */
    public Shift() {
        // בנאי ריק חובה ל-Firebase
    }

    /**
     * בנאי מלא ליצירת משמרת חדשה מתוך האפליקציה (ע"י המנהל).
     *
     * @param shiftId - מזהה ייחודי למשמרת.
     * @param startTime - זמן התחלה.
     * @param endTime - זמן סיום.
     * @param requiredWorkers - כמות עובדים דרושה.
     * @param notes - הערות מיוחדות למשמרת.
     */
    public Shift(String shiftId, long startTime, long endTime, int requiredWorkers, String notes) {
        this.shiftId = shiftId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredWorkers = requiredWorkers;
        this.notes = notes;

        // אתחול רשימות (Initialization):
        // אנחנו יוצרים רשימות ריקות (ArrayList) כדי שכאשר נוסיף עובד ראשון,
        // הרשימה לא תהיה null (דבר שיגרום לקריסת האפליקציה).
        this.assignedUserIds = new ArrayList<>();
        this.assignedUserNames = new ArrayList<>();
        this.pendingUserIds = new ArrayList<>();
        this.pendingUserNames = new ArrayList<>();
    }

    // --- Getters and Setters ---
    // פונקציות גישה המאפשרות ל-Firebase לקרוא ולכתוב את הנתונים.

    public String getShiftId() { return shiftId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getRequiredWorkers() { return requiredWorkers; }
    public void setRequiredWorkers(int requiredWorkers) { this.requiredWorkers = requiredWorkers; }

    // גישה לרשימת המשובצים (ID)
    public List<String> getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(List<String> assignedUserIds) { this.assignedUserIds = assignedUserIds; }

    // גישה לרשימת המשובצים (שמות)
    public List<String> getAssignedUserNames() { return assignedUserNames; }
    public void setAssignedUserNames(List<String> assignedUserNames) { this.assignedUserNames = assignedUserNames; }

    // גישה לרשימת הממתינים (ID)
    public List<String> getPendingUserIds() { return pendingUserIds; }
    public void setPendingUserIds(List<String> pendingUserIds) { this.pendingUserIds = pendingUserIds; }

    // גישה לרשימת הממתינים (שמות)
    public List<String> getPendingUserNames() { return pendingUserNames; }
    public void setPendingUserNames(List<String> pendingUserNames) { this.pendingUserNames = pendingUserNames; }

    // --- Getter & Setter לשדה הערות ---
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}