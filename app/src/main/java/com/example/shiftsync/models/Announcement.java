package com.example.shiftsync.models;

/**
 * מחלקה המייצגת מודל נתונים של "הודעה" (Announcement).
 * מחלקה זו משמשת כ-POJO (Plain Old Java Object) למיפוי נתונים מול Firebase Firestore.
 * כל מופע של המחלקה מייצג מסמך אחד באוסף "announcements" במסד הנתונים.
 */
public class Announcement {

    // --- משתנים (Fields) ---

    // המזהה הייחודי של ההודעה (Document ID).
    // משמש אותנו כדי למצוא את ההודעה הספציפית לצורך מחיקה או עריכה בעתיד.
    private String id;

    // כותרת ההודעה (למשל: "עדכון שעות פעילות").
    private String title;

    // תוכן ההודעה המלא (הטקסט שהמנהל כתב).
    private String content;

    // חותמת זמן (Timestamp) במילי-שניות (long).
    // נשמר כמספר (למשל 1704098200000) כדי שנוכל למיין את ההודעות מהחדשה לישנה בקלות.
    private long timestamp;

    // שם המנהל שכתב את ההודעה.
    // אנחנו שומרים את השם כאן כדי שלא נצטרך לבצע שליפה נוספת (Query) לטבלת המשתמשים רק כדי להציג את השם.
    private String authorName;

    // --- בנאים (Constructors) ---

    /**
     * בנאי ריק (Empty Constructor).
     * חובה! Firebase דורש בנאי ריק כדי להמיר אוטומטית את הנתונים מהמסד (JSON)
     * לאובייקט Java. אם נמחק אותו, האפליקציה תקרוס בעת טעינת נתונים.
     */
    public Announcement() { }

    /**
     * בנאי מלא.
     * משמש אותנו בקוד (ב-Activity של המנהל) כשאנחנו יוצרים הודעה חדשה
     * לפני שאנחנו שולחים אותה ל-Firebase.
     *
     * @param id - ה-ID שנוצר (בדרך כלל ע"י UUID).
     * @param title - הכותרת שהוזנה.
     * @param content - התוכן שהוזן.
     * @param timestamp - הזמן הנוכחי (System.currentTimeMillis()).
     * @param authorName - השם של המנהל המחובר.
     */
    public Announcement(String id, String title, String content, long timestamp, String authorName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.authorName = authorName;
    }

    // --- Getters and Setters ---
    // פונקציות אלו מאפשרות גישה (קריאה וכתיבה) למשתנים הפרטיים.
    // Firebase משתמש בהן כדי לקרוא את המידע לפני שמירה וכדי להזין מידע בעת קריאה.

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
}