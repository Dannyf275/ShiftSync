package com.example.shiftsync.models;

/**
 * מחלקת עזר (Helper Class) לייצוג שורה בודדת במסך "בקשות לאישור" של המנהל.
 * * למה צריך את המחלקה הזו?
 * ברשימת הבקשות, כל שורה מייצגת שילוב של:
 * 1. המשמרת שעליה מדובר (Shift).
 * 2. העובד הספציפי שביקש אותה (User ID + Name).
 * * מכיוון שמשמרת אחת יכולה להכיל כמה בקשות מעובדים שונים, אנחנו "מפרקים" את זה
 * לאובייקטים שטוחים שקל להציג ב-RecyclerView.
 */
public class ShiftRequestItem {

    // --- שדות (Fields) ---

    // אובייקט המשמרת המלא.
    // אנו שומרים את כל האובייקט כדי שנוכל להציג את התאריך והשעה,
    // וכדי שנוכל לגשת ל-Shift ID בעת אישור או דחייה של הבקשה.
    private Shift shift;

    // המזהה הייחודי (UID) של העובד שביקש את המשמרת.
    // נדרש כדי שנוכל לבצע את הפעולה ב-Firestore:
    // להעביר אותו מרשימת ה-pending لرשימת ה-assigned.
    private String userId;

    // שם העובד המבקש.
    // נדרש אך ורק לתצוגה על המסך (כדי שהמנהל ידע את מי הוא מאשר),
    // בלי שנצטרך לבצע קריאה נוספת לשרת כדי להביא את השם.
    private String userName;

    // --- בנאי (Constructor) ---

    /**
     * בנאי ליצירת פריט בקשה חדש.
     * בדרך כלל נקרא לו בתוך הלולאה שסורקת את המשמרות ומייצרת את הרשימה למנהל.
     *
     * @param shift - המשמרת הרלוונטית.
     * @param userId - מזהה העובד המבקש.
     * @param userName - שם העובד המבקש.
     */
    public ShiftRequestItem(Shift shift, String userId, String userName) {
        this.shift = shift;
        this.userId = userId;
        this.userName = userName;
    }

    // --- Getters ---
    // פונקציות לקריאת הנתונים (אין צורך ב-Setters כי המידע הזה לא משתנה בתוך הרשימה).

    public Shift getShift() { return shift; }

    public String getUserId() { return userId; }

    public String getUserName() { return userName; }
}