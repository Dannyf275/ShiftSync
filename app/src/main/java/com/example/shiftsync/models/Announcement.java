package com.example.shiftsync.models;

/**
 *הודעות של לוח ההודעות
 */
public class Announcement {


    // מזהה
    private String id;

    // כותרת
    private String title;

    // תוכן ההודעה
    private String content;

    // זמן כתיבה לסדר הצגה
    private long timestamp;

    // שם כותב ההודעה לשליפה מהירה
    private String authorName;

    //  בנאים

    // בנאי ריק לממשק עם הפיירבייס
    public Announcement() { }


    //בנאי מלא

    public Announcement(String id, String title, String content, long timestamp, String authorName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.authorName = authorName;
    }

    // getters וsetters

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