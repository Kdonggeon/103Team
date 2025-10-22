package com.team103.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "followups")
public class FollowUp {

    @Id
    private String id;

    private String questionId;
    private String content;
    private String author;       // userId
    private String authorRole;   // "student" or "parent"
    private Date createdAt;
    private boolean deleted;

    // 표시용 필드: DB 저장 안 함
    @Transient
    private String studentName;

    @Transient
    private String parentName;

    public FollowUp() {}

    // === JSON에 _id도 함께 내려가도록 별칭 게터 ===
    @JsonProperty("_id")
    public String get_id() {
        return this.id;
    }

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

    @JsonProperty("studentName")
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    @JsonProperty("parentName")
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
}
