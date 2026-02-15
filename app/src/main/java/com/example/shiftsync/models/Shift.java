package com.example.shiftsync.models;

import java.util.ArrayList;
import java.util.List;

// משמרת
public class Shift {

    // מזהה משמרת
    private String shiftId;

    // זמן התחלה
    private long startTime;

    // זמן סיום
    private long endTime;

    // מספר עובדים נדרש
    private int requiredWorkers;

    // מזהי עובדים ששובצו למשמרת
    private List<String> assignedUserIds;

    // שמות עובדים ששובצו למשמרת
    private List<String> assignedUserNames;

    // מזהי עובדים שביקשו להרשם למשמרת
    private List<String> pendingUserIds;

    // שמות עובדים שמבקשים להרשם למשמרת
    private List<String> pendingUserNames;

    // הערות למשמרת
    private String notes;

    // בנאים

    //בנאי ריק
    public Shift() {
        // בנאי ריק חובה ל-Firebase
    }

    //בנאי מלא
    public Shift(String shiftId, long startTime, long endTime, int requiredWorkers, String notes) {
        this.shiftId = shiftId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredWorkers = requiredWorkers;
        this.notes = notes;

        // אתחול רשימות (שלא נקבל NULL ונגרום לקריסה)
        this.assignedUserIds = new ArrayList<>();
        this.assignedUserNames = new ArrayList<>();
        this.pendingUserIds = new ArrayList<>();
        this.pendingUserNames = new ArrayList<>();
    }

    // getters וsetters

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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}