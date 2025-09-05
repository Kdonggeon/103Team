package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Question {

    private String id;
    private String title;
    private String content;
    private String author;
    private String createdAt;
    private int academyNumber;
    @SerializedName("teacherNames")
    private List<String> teacherNames;

    // ✅ 추가: 학원명(서버가 내려주면 사용, 없으면 null)
    private String academyName;

    private List<String> recentResponderNames;
    private int unreadCount;

    public Question() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }


    public List<String> getTeacherNames() { return teacherNames; }
    public void setTeacherNames(List<String> v) { this.teacherNames = v; }
    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }

    public List<String> getRecentResponderNames(){ return recentResponderNames; }
    public void setRecentResponderNames(List<String> v){ this.recentResponderNames = v; }
    public int getUnreadCount(){ return unreadCount; }
    public void setUnreadCount(int v){ this.unreadCount = v; }
}
