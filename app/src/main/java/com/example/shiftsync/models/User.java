package com.example.shiftsync.models;

/**
 * מחלקה המייצגת "משתמש" (User) במערכת.
 * מחלקה זו משמשת כמודל נתונים (Model/POJO) עבור מסד הנתונים Firestore.
 * כל מסמך באוסף "users" ב-Firebase ממופה למופע (Instance) של המחלקה הזו.
 */
public class User {

    // --- קבועים (Constants) ---
    // משתנים סטטיים שמגדירים את סוגי המשתמשים האפשריים.
    // השימוש בקבועים מונע שגיאות כתיב (למשל, כדי שלא נכתוב במקום אחד "manager" ובמקום אחר "Manager").
    public static final String ROLE_EMPLOYEE = "employee";
    public static final String ROLE_MANAGER = "manager";

    // --- שדות הנתונים (Fields) ---

    // המזהה הייחודי של המשתמש (User ID).
    // זהה ל-UID שמקבלים מ-Firebase Authentication בעת ההרשמה.
    // משמש כמפתח הראשי של המסמך ב-Firestore.
    private String uid;

    // שם מלא של המשתמש (להצגה באפליקציה).
    private String fullName;

    // מספר תעודת זהות (לצורך זיהוי ייחודי וניהול עובדים).
    private String idNumber;

    // כתובת האימייל (משמשת להתחברות וכמזהה קשר).
    private String email;

    // תפקיד המשתמש: מכיל את אחד מהקבועים למעלה (ROLE_EMPLOYEE או ROLE_MANAGER).
    // קובע לאיזה מסך המשתמש יופנה לאחר ההתחברות (EmployeeActivity או ManagerActivity).
    private String role;

    // שכר שעתי (רלוונטי בעיקר לעובדים).
    // משמש לחישוב השכר החודשי המשוער.
    private double hourlyRate;

    // דגל בוליאני לבדיקה האם זו התחברות ראשונה (אופציונלי, לשימוש עתידי ל-Tutorial וכו').
    private boolean isFirstLogin;

    // --- שדה תמונת פרופיל ---
    // במקום לשמור קישור (URL) לאחסון חיצוני, אנו שומרים את התמונה עצמה
    // כמחרוזת טקסט ארוכה (Base64 String).
    // זה מפשט את העבודה כי לא צריך לנהל הרשאות ב-Firebase Storage.
    private String profileImage;

    // --- בנאים (Constructors) ---

    /**
     * בנאי ריק (Empty Constructor).
     * חובה! Firebase חייב את הבנאי הזה כדי לדעת איך ליצור אובייקט User ריק
     * ולמלא אותו בנתונים שהוא מוריד מהשרת.
     */
    public User() {
    }

    /**
     * בנאי מלא.
     * משמש אותנו במסך ההרשמה (RegisterActivity) כדי ליצור את המשתמש החדש
     * לפני השמירה הראשונית ב-Firebase.
     *
     * @param uid - המזהה מ-Auth.
     * @param fullName - שם מלא.
     * @param idNumber - ת.ז.
     * @param email - אימייל.
     * @param role - תפקיד.
     * @param hourlyRate - שכר התחלתי.
     */
    public User(String uid, String fullName, String idNumber, String email, String role, double hourlyRate) {
        this.uid = uid;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.email = email;
        this.role = role;
        this.hourlyRate = hourlyRate;
        this.isFirstLogin = true; // ברירת מחדל: זו התחברות ראשונה
        this.profileImage = null; // ברירת מחדל: אין תמונה
    }

    // --- Getters and Setters ---
    // פונקציות סטנדרטיות המאפשרות גישה ושינוי של הנתונים.
    // Firebase משתמש בהן כדי לקרוא (Serialize) ולכתוב (Deserialize) את הנתונים.

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public boolean isFirstLogin() { return isFirstLogin; }
    public void setFirstLogin(boolean firstLogin) { isFirstLogin = firstLogin; }

    // פונקציות גישה לשדה התמונה החדש
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}