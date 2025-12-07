package com.example.shiftsync.models;

import java.util.ArrayList;
import java.util.List;

/**
 * מודל נתונים של משמרת.
 * עודכן כדי לתמוך בתהליך אישור:
 * - assignedUserIds: עובדים שאושרו סופית.
 * - pendingUserIds: עובדים שממתינים לאישור מנהל.
 */
public class Shift {
    private String shiftId;
    private long startTime;
    private long endTime;
    private int requiredWorkers;

    // רשימות למשובצים סופית (Approved)
    private List<String> assignedUserIds;
    private List<String> assignedUserNames;

    // רשימות לממתינים לאישור (Pending Requests)
    private List<String> pendingUserIds;
    private List<String> pendingUserNames;

    // בנאי ריק (חובה ל-Firebase)
    public Shift() {
        // אתחול כל הרשימות כדי למנוע NullPointerException
        this.assignedUserIds = new ArrayList<>();
        this.assignedUserNames = new ArrayList<>();
        this.pendingUserIds = new ArrayList<>();
        this.pendingUserNames = new ArrayList<>();
    }

    // בנאי ליצירה חדשה
    public Shift(String shiftId, long startTime, long endTime, int requiredWorkers) {
        this.shiftId = shiftId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredWorkers = requiredWorkers;

        this.assignedUserIds = new ArrayList<>();
        this.assignedUserNames = new ArrayList<>();
        this.pendingUserIds = new ArrayList<>();
        this.pendingUserNames = new ArrayList<>();
    }

    // --- Getters and Setters ---

    public String getShiftId() { return shiftId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getRequiredWorkers() { return requiredWorkers; }
    public void setRequiredWorkers(int requiredWorkers) { this.requiredWorkers = requiredWorkers; }

    public List<String> getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(List<String> assignedUserIds) { this.assignedUserIds = assignedUserIds; }

    public List<String> getAssignedUserNames() { return assignedUserNames; }
    public void setAssignedUserNames(List<String> assignedUserNames) { this.assignedUserNames = assignedUserNames; }

    public List<String> getPendingUserIds() { return pendingUserIds; }
    public void setPendingUserIds(List<String> pendingUserIds) { this.pendingUserIds = pendingUserIds; }

    public List<String> getPendingUserNames() { return pendingUserNames; }
    public void setPendingUserNames(List<String> pendingUserNames) { this.pendingUserNames = pendingUserNames; }

    // פונקציית עזר: המשמרת נחשבת מלאה רק אם כמות המאושרים הגיעה ליעד
    public boolean isFull() {
        if (assignedUserIds == null) return false;
        return assignedUserIds.size() >= requiredWorkers;
    }
}