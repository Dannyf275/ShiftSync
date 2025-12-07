package com.example.shiftsync.models;

/**
 * מחלקת עזר המייצגת "בקשה בודדת" ברשימת הבקשות של המנהל.
 * היא מחברת בין המשמרת הספציפית לבין העובד שביקש אותה.
 */
public class ShiftRequestItem {
    private Shift shift;       // אובייקט המשמרת המלא
    private String userId;     // ה-UID של העובד המבקש
    private String userName;   // שם העובד המבקש

    public ShiftRequestItem(Shift shift, String userId, String userName) {
        this.shift = shift;
        this.userId = userId;
        this.userName = userName;
    }

    public Shift getShift() { return shift; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
}