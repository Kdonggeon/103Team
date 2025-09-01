package com.mobile.greenacademypartner.model;

public class FollowUp {
    private String id;
    private String questionId;
    private String author;
    private String authorRole; // "student", "parent"
    private String content;
    private String createdAt;

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
}
