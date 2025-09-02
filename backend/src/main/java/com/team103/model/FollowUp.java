package com.team103.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;      // ✅ 추가
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "followups")
public class FollowUp {

    @Id
    @JsonProperty("id")
    private String id;

    private String questionId;
    private String content;
    private String author;      // userId
    private String authorRole;  // "student" or "parent"
    private Date createdAt;
    private boolean deleted;

    // ✅ 응답 전용 필드 (DB 저장 안 함)
    @JsonProperty("학생이름")
    private String studentName;

    @JsonProperty("학부모이름")
    private String parentName;

    public FollowUp() {}

    // getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
}
