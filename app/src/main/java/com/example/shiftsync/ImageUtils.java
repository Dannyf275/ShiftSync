package com.example.shiftsync;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * מחלקת עזר (Utility Class) לטיפול בתמונות.
 * המחלקה מספקת פונקציות סטטיות להמרה בין תמונה (Bitmap) לטקסט (Base64),
 * וכן פונקציה קריטית להקטנת תמונות כדי למנוע חריגה ממגבלות הגודל של Firestore.
 */
public class ImageUtils {

    /**
     * פונקציה להמרת תמונה (Bitmap) למחרוזת טקסט (String/Base64).
     * שימוש: לפני שמירה ב-Firebase, חובה להמיר את התמונה לטקסט.
     *
     * @param bitmap - התמונה המקורית שרוצים לשמור.
     * @return String - המחרוזת המוצפנת (Base64) שמייצגת את התמונה.
     */
    public static String bitmapToString(Bitmap bitmap) {
        // בדיקת תקינות: אם אין תמונה, מחזירים null
        if (bitmap == null) return null;

        // יצירת "זרם" (Stream) לכתיבת בתים בזיכרון
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // דחיסה (Compress):
        // 1. פורמט JPEG (חוסך מקום לעומת PNG).
        // 2. איכות 70% (פשרה טובה בין איכות לגודל הקובץ).
        // התוצאה נכתבת לתוך ה-baos.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);

        // המרה למערך של בתים (byte array)
        byte[] b = baos.toByteArray();

        // המרה סופית למחרוזת Base64 (פורמט טקסטואלי שמייצג מידע בינארי)
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    /**
     * פונקציה להמרת מחרוזת טקסט (Base64) חזרה לתמונה (Bitmap).
     * שימוש: כששולפים את המידע מ-Firebase, מקבלים טקסט ורוצים להציג אותו ב-ImageView.
     *
     * @param encodedString - המחרוזת המוצפנת שהגיעה מהמסד נתונים.
     * @return Bitmap - התמונה המוכנה להצגה (או null אם הייתה שגיאה).
     */
    public static Bitmap stringToBitmap(String encodedString) {
        try {
            // פעולה הפוכה: המרה מטקסט למערך בתים
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);

            // שימוש במפענח של אנדרואיד ליצירת Bitmap מתוך הבתים
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch (Exception e) {
            // במקרה של מחרוזת פגומה או שגיאה אחרת, נחזיר null כדי לא להקריס את האפליקציה
            e.getMessage();
            return null;
        }
    }

    /**
     * פונקציה להקטנת תמונה (Resize) תוך שמירה על יחס רוחב/גובה (Aspect Ratio).
     * חובה להשתמש בה לפני השמירה!
     * הסיבה: Firestore קורס אם מנסים לשמור מסמך גדול מ-1MB. תמונות מצלמה הן ענקיות.
     *
     * @param image - התמונה המקורית.
     * @param maxSize - הגודל המקסימלי בפיקסלים (למשל 500). זה יהיה הגודל של הצלע הארוכה ביותר.
     * @return Bitmap - התמונה המוקטנת.
     */
    public static Bitmap resizeBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        // חישוב היחס בין הרוחב לגובה
        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1) {
            // התמונה לרוחב (Landscape): הרוחב גדול מהגובה.
            // נקבע את הרוחב למקסימום, ונחשב את הגובה בהתאם ליחס.
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            // התמונה לאורך (Portrait) או מרובעת: הגובה גדול או שווה לרוחב.
            // נקבע את הגובה למקסימום, ונחשב את הרוחב בהתאם ליחס.
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        // יצירת תמונה חדשה מוקטנת (הפרמטר true בסוף דואג לאיכות טובה יותר בהקטנה - Filter)
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}