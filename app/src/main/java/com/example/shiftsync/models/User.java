package com.example.shiftsync.models;

/**
 * מחלקה זו (Model) מייצגת ישות של "משתמש" במערכת.
 * היא משמשת כ-Data Transfer Object (DTO) לשמירה ושליפה של נתונים מ-Firebase Firestore.
 * המבנה הזה מאפשר לנו להמיר בקלות מסמך JSON מהענן לאובייקט Java ולהפך.
 */
public class User {

    // --- שדות הנתונים (Fields) ---
    // משתנים אלו מוגדרים כ-private כדי לממש עקרון ה-Encapsulation (כימוס).
    // הגישה אליהם מתבצעת רק דרך Getters ו-Setters.

    // המזהה הייחודי כפי שנוצר ע"י Firebase Authentication.
    // משמש לקישור בין פרטי ההתחברות (אימייל/סיסמה) לבין נתוני המשתמש ב-Firestore.
    private String uid;

    // שמו המלא של המשתמש (לתצוגה ברשימות וכותרות).
    private String fullName;

    // כתובת האימייל של המשתמש (משמשת גם להתחברות).
    private String email;

    // תפקיד המשתמש במערכת. הערכים האפשריים הם בד"כ "manager" או "employee".
    private String role;

    // שכר שעתי. רלוונטי בעיקר לעובדים לצורך חישוב שכר עתידי.
    // משתמשים ב-double כדי לאפשר מספרים עשרוניים (למשל 30.5).
    private double hourlyRate;

    // תעודת זהות - שדה ייחודי לזיהוי העובד (נוסף כחלק מהדרישות החדשות).
    private String idNumber;

    // --- קבועים (Constants) ---
    // שימוש במשתנים סטטיים מונע שגיאות כתיב בקוד ("Magic Strings").
    // במקום לכתוב "manager" ידנית בכל מקום, משתמשים ב-user.ROLE_MANAGER.
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_EMPLOYEE = "employee";

    // --- בנאים (Constructors) ---

    /**
     * בנאי ריק (Empty Constructor).
     * חובה! Firebase זקוק לבנאי הזה כדי ליצור מופע של המחלקה
     * לפני שהוא מזין לתוכה את הנתונים מהענן (Deserialization).
     */
    public User() { }

    /**
     * בנאי מלא (Full Constructor).
     * משמש אותנו כאשר אנחנו יוצרים משתמש חדש באפליקציה (למשל במסך הרשמה)
     * ורוצים לאתחל אותו עם כל הנתונים לפני השליחה ל-Firestore.
     *
     * @param uid מזהה ייחודי
     * @param fullName שם מלא
     * @param email אימייל
     * @param role תפקיד
     * @param hourlyRate שכר לשעה
     * @param idNumber תעודת זהות
     */
    public User(String uid, String fullName, String email, String role, double hourlyRate, String idNumber) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.hourlyRate = hourlyRate;
        this.idNumber = idNumber;
    }

    // --- Getters and Setters ---
    // פונקציות אלו מאפשרות קריאה (Get) וכתיבה (Set) של המשתנים הפרטיים.
    // Firebase משתמש בהן אוטומטית כדי למפות את שדות ה-JSON למשתני המחלקה.

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
}