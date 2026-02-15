package com.example.shiftsync.models;

//בקשות לאישור
public class ShiftRequestItem {


    // המשמרת עבורה נוצרה הבקשה
    private Shift shift;

    // מזהה העובד שביקש שיבוץ

    private String userId;

    // שם העובד שביקש שיבוץ
    private String userName;

    // בנאי

    public ShiftRequestItem(Shift shift, String userId, String userName) {
        this.shift = shift;
        this.userId = userId;
        this.userName = userName;
    }

    // getters

    public Shift getShift() { return shift; }

    public String getUserId() { return userId; }

    public String getUserName() { return userName; }
}