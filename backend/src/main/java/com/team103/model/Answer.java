package com.team103.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "answers")
public class Answer {
	@Id
	@JsonProperty("id")
	private String id;
    private String questionId; // 연관된 Question의 id
    private String content;
    private String author;     // 교사 아이디
    private Date createdAt;
    private boolean deleted = false;
    
    
    public Answer() {
        this.createdAt = new Date();
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
    public boolean isDeleted() {return deleted;}
    public void setDeleted(boolean deleted) {this.deleted = deleted; }
}
