package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document(collection = "questions")
public class Question {
    @Id
    private String id;
    private String title;
    private String content;
    private String author;     // 학생 또는 학부모 아이디
    private Date createdAt;
    private String authorRole;
    private int academyNumber;
    private java.util.List<String> teacherNames; // 이 질문에 답변한 교사 이름들(중복 제거, 순서 유지)
    private String academyName;
    @Field("room")
    private Boolean room;
    @Transient
    private java.util.List<String> recentResponderNames;
    @Transient
    private int unreadCount;     
    private String roomParentId;
    private String roomStudentId;   // 학생별 1:1 방 키
    public Question() {
        this.createdAt = new Date();
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public int getAcademyNumber() { return academyNumber; }
    public void setAcademyNumber(int academyNumber) { this.academyNumber = academyNumber; }

    public java.util.List<String> getTeacherNames() { return teacherNames; }
    public void setTeacherNames(java.util.List<String> teacherNames) { this.teacherNames = teacherNames; }

    public String getAcademyName() { return academyName; }
    public void setAcademyName(String academyName) { this.academyName = academyName; }

    public Boolean getRoom() { return room; }
    public void setRoom(Boolean room) { this.room = room; }
    
    public java.util.List<String> getRecentResponderNames(){ return recentResponderNames; }
    public void setRecentResponderNames(java.util.List<String> v){ this.recentResponderNames = v; }
    
    public int getUnreadCount(){ return unreadCount; }
    public void setUnreadCount(int v){ this.unreadCount = v; }
    
    public String getRoomStudentId() { return roomStudentId; }
    public void setRoomStudentId(String roomStudentId) { this.roomStudentId = roomStudentId; }
    
    public String getRoomParentId() {
        return roomParentId;
    }

    public void setRoomParentId(String roomParentId) {
        this.roomParentId = roomParentId;
    }
}
