package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Transient;

@Document(collection = "answers")
public class Answer {

    @Id
    private String id;                 // ← 굳이 @JsonProperty("id") 붙일 필요 없습니다(기본이 id)

    private String questionId;         // 연관된 Question의 id
    private String content;
    private String author;             // 교사 아이디
    private Date createdAt;
    private boolean deleted = false;

    // 응답 전용 필드: DB에는 저장하지 않고 JSON으로만 내려감
    @Transient
    private String teacherName;

    public Answer() {
        this.createdAt = new Date();
    }

    // === Alias: JSON에 _id도 함께 내려가도록 ===
    @JsonProperty("_id")
    public String get_id() {
        return this.id;
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    // teacherName은 JSON 키를 "teacherName"으로(프런트 기대치와 일치)
    @JsonProperty("teacherName")
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
}
