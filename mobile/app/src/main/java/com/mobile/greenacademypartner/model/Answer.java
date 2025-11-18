package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;

public class Answer {

    private String id;
    private String questionId;
    private String author;      // 교사 ID
    private String content;
    @SerializedName(value = "createdAt", alternate = {"created_at","CreatedAt","Created_At"})
    private String createdAt;

    // 백엔드 응답 키: "교사이름"
    @SerializedName(value = "teacherName", alternate = {"교사이름"})
    private String teacherName;

    public Answer() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
}
