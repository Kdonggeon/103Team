package com.mobile.greenacademypartner.model;

import com.google.gson.annotations.SerializedName;

public class FollowUp {

    private String id;
    private String questionId;
    private String author;       // 작성자 ID (학생/학부모)
    private String authorRole;   // "student" or "parent"
    private String content;
    @SerializedName(value = "createdAt", alternate = {"created_at","CreatedAt","Created_At"})
    private String createdAt;

    // 서버가 내려주는 표시용 이름
    @SerializedName("학생이름")
    private String studentName;

    @SerializedName("학부모이름")
    private String parentName;

    public FollowUp() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
}
