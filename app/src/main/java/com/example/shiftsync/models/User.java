package com.example.shiftsync.models;

//משץמש במערכת
public class User {

    // סוגי משתמש כקבועים למניעת שגיאות
    public static final String ROLE_EMPLOYEE = "employee";
    public static final String ROLE_MANAGER = "manager";

    // נתוני משתמש

    // מזהה ייחודי
    private String uid;

    // שם מלא של המשתמש
    private String fullName;

    //  תעודת זהות
    private String idNumber;

    // כתובת אימייל
    private String email;

    // תפקיד המשתמש: מכיל את אחד מהקבועים למעלה (ROLE_EMPLOYEE או ROLE_MANAGER)
    // קובע לאיזה מסך המשתמש יופנה לאחר ההתחברות (EmployeeActivity או ManagerActivity)
    private String role;

    // שכר שעתי
    private double hourlyRate;

    // בדיקה האם התחברות ראשונה
    private boolean isFirstLogin;

    // תמונת פרופיל - בפורמט סטרינג לשמירה פשוטה ללא תשלום
    private String profileImage;

    // בנאים

    //בנאי ריק
    public User() {
    }

    //בנאי מלא
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

    // getters וsetters

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


    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}